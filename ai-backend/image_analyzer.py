# Gerekli kütüphanelerin import edilmesi
import base64
import io
import os
import json
import torch
from PIL import Image
from transformers import AutoImageProcessor, SiglipForImageClassification
from typing import Dict, List, Optional
from datetime import datetime, timedelta
from uuid import uuid4

from dotenv import load_dotenv
load_dotenv()

import google.generativeai as genai
genai = None

# Sınıflandırma için kullanılacak Hugging Face modelinin adı
CLASSIFIER_MODEL_NAME = "prithivMLmods/tooth-agenesis-siglip2"

# Model çıktısındaki ID'leri Türkçe ve İngilizce etiketlere eşler
ID_TO_LABEL = {
    "0": {"tr": "Diş Taşı (Calculus)", "en": "Calculus"},
    "1": {"tr": "Diş Çürüğü (Karies)", "en": "Caries"},
    "2": {"tr": "Diş Eti İltihabı (Gingivitis)", "en": "Gingivitis"},
    "3": {"tr": "Aft (Ağız Yarası)", "en": "Mouth Ulcer"},
    "4": {"tr": "Diş Renklenmesi", "en": "Tooth Discoloration"},
    "5": {"tr": "Hipodonti (Eksik Diş)", "en": "Hypodontia"},
}

# Tespit edilen durumlara göre önerilecek YouTube video arama linkleri
VIDEO_LINKS = {
    "Diş Taşı (Calculus)": "https://www.youtube.com/results?search_query=di%C5%9F+ta%C5%9F%C4%B1+nasil+temizlenir",
    "Diş Çürüğü (Karies)": "https://www.youtube.com/results?search_query=di%C5%9F+%C3%A7%C3%BCr%C3%BC%C4%9F%C3%BC+%C3%B6nleme",
    "Diş Eti İltihabı (Gingivitis)": "https://www.youtube.com/results?search_query=di%C5%9F+eti+iltihab%C4%B1+bak%C4%B1m%C4%B1",
    "Aft (Ağız Yarası)": "https://www.youtube.com/results?search_query=aft+nedir+tedavisi",
    "Diş Renklenmesi": "https://www.youtube.com/results?search_query=di%C5%9F+renklenmesi+nas%C4%B1l+ge%C3%A7er",
    "Hipodonti (Eksik Diş)": "https://www.youtube.com/results?search_query=hipodonti+nedir+tedavisi",
}

# Diş görüntüsünü analiz eden, yorumlayan ve bakım planı oluşturan ana sınıf
class DentalImageAnalyzer:
    """Görüntü sınıflandırma ve Gemini ile metin üretme yeteneklerini birleştirir."""

    def __init__(self):
        """Sınıf başlatıldığında görüntü sınıflandırma ve Gemini modellerini yükler."""
        self.image_processor = None
        self.image_classifier = None
        self.gemini_model = None
        self.user_memory = {}  
        
        # Görüntü sınıflandırma modelini yükler
        try:
            self.image_classifier = SiglipForImageClassification.from_pretrained(CLASSIFIER_MODEL_NAME)
            self.image_processor = AutoImageProcessor.from_pretrained(CLASSIFIER_MODEL_NAME)
            print(f"Image classifier loaded: {CLASSIFIER_MODEL_NAME}")
        except Exception as e:
            print(f"Image classifier load error: {e}")
        
        # Metin üretimi için Gemini modelini yükler
        if genai is not None:
            try:
                gemini_api_key = os.getenv("GEMINI_API_KEY")
                if gemini_api_key:
                    genai.configure(api_key=gemini_api_key)
                    self.gemini_model = genai.GenerativeModel("gemini-1.5-flash")
                    print("Gemini NLG enabled: gemini-1.5-flash")
                else:
                    self.gemini_model = None 
                    print("GEMINI_API_KEY not set; using rule-based summary")
            except Exception as e:
                print(f"Gemini init error: {e}")

    def analyze_image(self, image_b64: str, user_id: Optional[str] = None, symptom: Optional[str] = None) -> Dict:
        """Base64 formatındaki diş görüntüsünü analiz eder ve sonuçları döndürür."""
        try:
            if self.image_processor is None or self.image_classifier is None:
                return {"error": "Görüntü sınıflandırma modeli yüklenemedi.", "success": False}
            
            # Base64 formatındaki görüntüyü çözer ve modelin anlayacağı formata getirir
            data_part = image_b64.split(',')[1] if ',' in image_b64 else image_b64
            image_data = base64.b64decode(data_part)
            image = Image.open(io.BytesIO(image_data)).convert("RGB")
            
            inputs = self.image_processor(images=image, return_tensors="pt")
            with torch.no_grad():
                outputs = self.image_classifier(**inputs)
                logits = outputs.logits
                probs = torch.nn.functional.softmax(logits, dim=1).squeeze().tolist()
            
            # %1'den düşük olasılıkları filtreler ve en yüksek olasılıklı sonuçları alır
            filtered_probs = {k: v for k, v in zip(ID_TO_LABEL.keys(), probs) if v >= 0.01}
            topk = sorted(filtered_probs.items(), key=lambda x: x[1], reverse=True)[:3]
            topk_labels = [f"{ID_TO_LABEL[k]['tr']}: {v*100:.1f}%" for k, v in topk]
            all_predictions = {
                "tr": {ID_TO_LABEL[k]["tr"]: round(v * 100, 2) for k, v in filtered_probs.items()},
                "en": {ID_TO_LABEL[k]["en"]: round(v * 100, 2) for k, v in filtered_probs.items()}
            }
            
            # En yüksek olasılıklı soruna göre özet ve haftalık plan oluşturur
            top_issue = ID_TO_LABEL[topk[0][0]]["tr"] if topk else None
            summary_data = self._generate_enhanced_summary(", ".join(topk_labels), top_issue, user_id)
            
            # Analiz sonucunu kullanıcının geçmişine kaydeder
            if user_id:
                if user_id not in self.user_memory:
                    self.user_memory[user_id] = []
                self.user_memory[user_id].append({"user": f"Scan: {', '.join(topk_labels)}", "ai": f"Plan for {top_issue or 'healthy'}."})
            
            # İstemciye gönderilecek sonuç sözlüğünü hazırlar
            result = {
                "top_predictions": topk_labels,  # Etiketler Türkçe
                "all_predictions": all_predictions,  # Türkçe ve İngilizce
                "dental_comment": summary_data["comment"],
                "weekly_plan": summary_data["plan"],
                "video_suggestion": VIDEO_LINKS.get(top_issue, "https://www.youtube.com/results?search_query=genel+di%C5%9F+bak%C4%B1m%C4%B1"),
                "success": True
            }
            
            # Eğer kullanıcı ek bir semptom belirtmişse, ona özel bir tavsiye ekler
            if symptom:
                symptom_advice = self._handle_symptom(symptom, top_issue or "Sağlıklı")
                result["symptom_advice"] = symptom_advice
            
            return result
            
        except Exception as e:
            return {"error": f"Analiz hatası: {str(e)}", "success": False}
    
    def _generate_enhanced_summary(self, findings_text: str, top_issue: Optional[str], user_id: Optional[str]) -> Dict:
        """Gemini kullanarak veya kural tabanlı bir yedek sistemle kişiselleştirilmiş özet ve plan oluşturur."""
        if self.gemini_model is not None:
            try:
                # Kullanıcının son 3 etkileşimini bellekten alarak prompt'a ekler
                history = self.user_memory.get(user_id, [])[-3:] if user_id and self.user_memory.get(user_id) else []
                history_str = "\n".join([f"Önceki: {msg['user']} -> {msg['ai']}" for msg in history]) if history else "Yeni kullanıcı."
                
                prompt = (
                    "Sen kişisel diş koçu asistanısın. Tarama sonuçlarına göre motive edici, detaylı TÜRKÇE özet ve 7 günlük kişiselleştirilmiş bakım planı üret. "
                    f"Tarihçe: {history_str}\n\n"
                    f"Sonuçlar: {findings_text}. Ana sorun: {top_issue or 'Sağlıklı görünüm'}. Semptom: {user_id or 'Yok'}\n\n"
                    "Özet: Riskleri, önerileri ve motivasyonu içersin (200 kelime max).\n"
                    "Plan: 7 gün için JSON array: [{'day': 'Pazartesi', 'task': 'Kısa, actionable görev'}]. Görevler çeşitlilikli, ana soruna odaklansın, hekime yönlendirsin.\n\n"
                    "Sadece JSON dön: {'comment': 'Özet metni', 'plan': [array]}"
                )
                
                gemini_resp = self.gemini_model.generate_content(prompt)
                response_text = getattr(gemini_resp, "text", "")
                try:
                    # Gemini'nin yanıtından JSON formatındaki veriyi ayıklar
                    json_start = response_text.find('{')
                    json_end = response_text.rfind('}') + 1
                    if json_start != -1 and json_end != 0:
                        parsed = json.loads(response_text[json_start:json_end])
                        # Planın 7 günlük olduğundan emin olur, değilse yedek planı kullanır
                        if len(parsed.get("plan", [])) != 7:
                            parsed["plan"] = self._generate_personalized_plan(top_issue)
                        return parsed
                except json.JSONDecodeError:
                    print("Gemini JSON parse failed, using fallback.")
            except Exception as e:
                print(f"Gemini error: {e}")

        # Gemini başarısız olursa veya mevcut değilse, kural tabanlı yedek sistemi çalıştırır
        base_comment = self._generate_detailed_comment(findings_text, top_issue)
        plan = self._generate_personalized_plan(top_issue)
        return {"comment": base_comment, "plan": plan}

    def _generate_detailed_comment(self, findings_text: str, top_issue: Optional[str]) -> str:
        """Kural tabanlı olarak, tespit edilen duruma göre detaylı bir açıklama metni oluşturur."""
        explanations = {
            "Diş Taşı (Calculus)": (
                "Diş taşları, diş yüzeyinde biriken sertleşmiş plaklardır. Diş eti hastalıklarına yol açabilir ve düzenli temizlik gerektirir. "
                "Risk: Diş eti çekilmesi ve enfeksiyon. "
                "Öneri: Diş hekiminizde profesyonel temizlik yaptırın ve günlük ağız hijyenine özen gösterin."
            ),
            "Diş Çürüğü (Karies)": (
                "Diş çürüğü, diş minesinde asitler nedeniyle oluşan oyuklardır. Erken müdahale edilmezse ağrı ve enfeksiyona yol açabilir. "
                "Risk: Çürüklerin ilerlemesi ve diş kaybı. "
                "Öneri: Şekerli gıdalardan kaçının, florürlü diş macunu kullanın ve diş hekiminize başvurun."
            ),
            "Diş Eti İltihabı (Gingivitis)": (
                "Diş eti iltihabı, diş etlerinde kızarıklık, şişlik ve kanamaya neden olur. Uygun bakım ile iyileşme mümkündür. "
                "Risk: Diş eti çekilmesi ve periodontitis. "
                "Öneri: Yumuşak bir fırça ile nazikçe fırçalayın ve diş ipi kullanın."
            ),
            "Aft (Ağız Yarası)": (
                "Aft, ağız içinde oluşan ağrılı yaralardır. Genellikle stres, vitamin eksikliği veya travmadan kaynaklanır. "
                "Risk: Konfor kaybı ve hassasiyet. "
                "Öneri: Tuzlu suyla gargara yapın ve tahriş edici yiyeceklerden kaçının."
            ),
            "Diş Renklenmesi": (
                "Diş renklenmesi, çay, kahve, sigara veya yapısal nedenlerden kaynaklanabilir. Estetik bir sorundur. "
                "Risk: Estetik kaygı ve özgüven kaybı. "
                "Öneri: Beyazlatıcı diş macunu kullanın ve profesyonel temizlik için diş hekiminize danışın."
            ),
            "Hipodonti (Eksik Diş)": (
                "Hipodonti, doğuştan bir veya daha fazla dişin eksik olmasıdır. Fonksiyonel ve estetik sorunlara yol açabilir. "
                "Risk: Çiğneme ve konuşma zorlukları. "
                "Öneri: Ortodontik veya protetik tedavi için diş hekiminize başvurun."
            ),
        }
        
        if top_issue and top_issue in explanations:
            top_prob = findings_text.split(",")[0].split(":")[1].strip() if findings_text else "N/A"
            secondary_issues = findings_text.split(",")[1:] if "," in findings_text else []
            comment = (
                f"Analiz sonuçlarınızı inceledim. Sonuçlara göre, en yüksek olasılıkla {top_prob} ile *{top_issue}* tespit edildi. "
                f"{explanations[top_issue]}\n\n"
            )
            if secondary_issues:
                comment += (
                    f"İkinci en olası bulgu ise {secondary_issues[0].split(':')[1].strip()} ile *{secondary_issues[0].split(':')[0].strip()}*. "
                    f"{explanations.get(secondary_issues[0].split(':')[0].strip(), 'Bu durum genellikle ciddi değildir, ancak dikkat edilmelidir.')}\n\n"
                )
            comment += (
                "Diğer olasılıklar %1'in altında olduğu için değerlendirmeye alınmamıştır.\n\n"
                "Size detaylı bir tedavi planı sunabilmem için en kısa sürede bir diş hekimine başvurmanız önemlidir. "
                "Erken teşhis ve tedavi ile bu durumu kolayca çözebiliriz. Sorularınız varsa lütfen çekinmeden sorun!"
            )
        else:
            comment = (
                "Analiz sonuçlarınızı inceledim. Dişleriniz genel olarak sağlıklı görünüyor! "
                f"Detaylı sonuçlar: {findings_text}\n\n"
                "Yine de düzenli diş hekimi kontrollerini ihmal etmeyin. "
                "Ağız hijyenine devam ederek bu sağlıklı durumu koruyabilirsiniz. Sorularınız varsa lütfen çekinmeden sorun!"
            )
        return comment

    def _generate_personalized_plan(self, top_issue: Optional[str]) -> list:
        """Kural tabanlı olarak, tespit edilen duruma özel 7 günlük bir bakım planı oluşturur."""
        day_tasks = {
            "Diş Taşı (Calculus)": [
                {"day": "Pazartesi", "task": "Diş hekiminizden profesyonel temizlik randevusu alın."},
                {"day": "Salı", "task": "Diş aralarını diş ipi veya ara yüz fırçasıyla temizleyin."},
                {"day": "Çarşamba", "task": "Elektrikli diş fırçası ile 2 dakika fırçalayın."},
                {"day": "Perşembe", "task": "Antibakteriyel ağız gargarası kullanın."},
                {"day": "Cuma", "task": "Şekerli ve yapışkan gıdalardan uzak durun."},
                {"day": "Cumartesi", "task": "Parmakla diş eti masajı yaparak kan dolaşımını artırın."},
                {"day": "Pazar", "task": "Diş fırçanızı kontrol edin, gerekirse yenileyin."},
            ],
            "Diş Çürüğü (Karies)": [
                {"day": "Pazartesi", "task": "Florürlü diş macunu ile sabah ve akşam 2 dakika fırçalayın."},
                {"day": "Salı", "task": "Diş ipi ile diş aralarını temizleyin."},
                {"day": "Çarşamba", "task": "Şekerli içecek ve yiyecek tüketimini azaltın."},
                {"day": "Perşembe", "task": "Bol su içerek ağız içini temiz tutun."},
                {"day": "Cuma", "task": "Diş hekiminizden çürük kontrolü için randevu alın."},
                {"day": "Cumartesi", "task": "Sebze ve meyve gibi sağlıklı atıştırmalıklar tüketin."},
                {"day": "Pazar", "task": "Ağız gargarası ile bakımınızı destekleyin."},
            ],
            "Diş Eti İltihabı (Gingivitis)": [
                {"day": "Pazartesi", "task": "Yumuşak kıllı fırça ile diş etlerinizi nazikçe fırçalayın."},
                {"day": "Salı", "task": "Diş ipi ile nazikçe diş aralarını temizleyin."},
                {"day": "Çarşamba", "task": "Antibakteriyel ağız gargarası kullanın."},
                {"day": "Perşembe", "task": "C vitamini açısından zengin gıdalar (portakal, kivi) tüketin."},
                {"day": "Cuma", "task": "Diş hekiminizden diş taşı temizliği randevusu alın."},
                {"day": "Cumartesi", "task": "Diş eti masajı ile kan dolaşımını destekleyin."},
                {"day": "Pazar", "task": "Diş eti kanaması devam ederse hekime başvurun."},
            ],
            "Aft (Ağız Yarası)": [
                {"day": "Pazartesi", "task": "Tuzlu suyla günde 2 kez gargara yapın."},
                {"day": "Salı", "task": "Asitli ve baharatlı yiyeceklerden kaçının."},
                {"day": "Çarşamba", "task": "Yumuşak kıllı fırça ile nazikçe fırçalayın."},
                {"day": "Perşembe", "task": "Soğuk ve yumuşak yiyecekler (yoğurt, smoothie) tüketin."},
                {"day": "Cuma", "task": "B12 ve C vitamini takviyesi almayı düşünün."},
                {"day": "Cumartesi", "task": "Stresi azaltmak için rahatlama teknikleri uygulayın."},
                {"day": "Pazar", "task": "Aft 1 haftadan uzun sürerse hekime başvurun."},
            ],
            "Diş Renklenmesi": [
                {"day": "Pazartesi", "task": "Çay, kahve ve sigara tüketimini azaltın."},
                {"day": "Salı", "task": "Beyazlatıcı diş macunu ile 2 dakika fırçalayın."},
                {"day": "Çarşamba", "task": "Diş hekiminizden profesyonel temizlik randevusu alın."},
                {"day": "Perşembe", "task": "Bol su içerek ağız içini temiz tutun."},
                {"day": "Cuma", "task": "Renklenmeye neden olan gıdalardan uzak durun."},
                {"day": "Cumartesi", "task": "Diş ipi ile diş aralarını temizleyin."},
                {"day": "Pazar", "task": "Beyazlatma seçenekleri için hekiminize danışın."},
            ],
            "Hipodonti (Eksik Diş)": [
                {"day": "Pazartesi", "task": "Ortodontik muayene için diş hekiminden randevu alın."},
                {"day": "Salı", "task": "Eksik dişlerin yerine tedavi seçeneklerini araştırın."},
                {"day": "Çarşamba", "task": "Yumuşak kıllı fırça ile dişlerinizi fırçalayın."},
                {"day": "Perşembe", "task": "Ağız hijyenine ekstra özen gösterin."},
                {"day": "Cuma", "task": "Diş hekiminizle tedavi planınızı görüşün."},
                {"day": "Cumartesi", "task": "Sağlıklı beslenmeye dikkat edin, kalsiyum alın."},
                {"day": "Pazar", "task": "Düzenli diş kontrolü için plan yapın."},
            ],
        }
        if top_issue in day_tasks:
            return day_tasks[top_issue]
        else:
            return [
                {"day": "Pazartesi", "task": "Sabah ve akşam 2 dakika diş fırçalayın."},
                {"day": "Salı", "task": "Diş ipi ile diş aralarını temizleyin."},
                {"day": "Çarşamba", "task": "Antibakteriyel ağız gargarası kullanın."},
                {"day": "Perşembe", "task": "Bol su içerek ağız hijyenini destekleyin."},
                {"day": "Cuma", "task": "Diş hekiminizden kontrol randevusu alın."},
                {"day": "Cumartesi", "task": "Sebze ve meyve gibi sağlıklı atıştırmalıklar tüketin."},
                {"day": "Pazar", "task": "Diş fırçanızı kontrol edin ve gerekirse yenileyin."},
            ]
    
    def _handle_symptom(self, symptom: str, top_issue: str) -> str:
        """Handle symptoms like 'Ağrım var'."""
        symptom_lower = symptom.lower()
        if "ağrı" in symptom_lower:
            return f"Ağrı: {top_issue} ile ilgili olabilir (zonklama mı?). Ibuprofen al, soğuk kompres uygula. Devam ederse dişçi!"
        elif "kanama" in symptom_lower:
            return "Kanama: Diş eti sorunu? Hafif fırçala, C vitamini artır. 2 günde geçmezse muayene."
        else:
            return "Semptom detaylandır: Ağrı tipi, şişlik? Ek bilgi ver."

# Global analyzer instance
analyzer = DentalImageAnalyzer()
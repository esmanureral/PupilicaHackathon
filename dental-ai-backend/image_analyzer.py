import base64
import io
import os
import json
import torch
from PIL import Image
from transformers import AutoImageProcessor, SiglipForImageClassification
from typing import Dict, List, Optional
from datetime import datetime, timedelta

try:
    from dotenv import load_dotenv
    load_dotenv()
except Exception:
    pass

try:
    import google.generativeai as genai
except Exception:
    genai = None

try:
    from langchain.tools import Tool
    from langchain.memory import ConversationBufferMemory
    from langchain.agents import AgentExecutor, create_react_agent
    from langchain.prompts import PromptTemplate
    from langchain_google_genai import ChatGoogleGenerativeAI
    LANGCHAIN_AVAILABLE = True
except ImportError:
    LANGCHAIN_AVAILABLE = False
    print("LangChain not available; agent features disabled.")

# Model configuration
CLASSIFIER_MODEL_NAME = "prithivMLmods/tooth-agenesis-siglip2"

# Label mapping (TÜRKÇE)
ID_TO_LABEL = {
    "0": "Diş Taşı (Calculus)",
    "1": "Diş Çürüğü (Karies)", 
    "2": "Diş Eti İltihabı (Gingivitis)",
    "3": "Aft (Ağız Yarası)",
    "4": "Diş Renklenmesi",
    "5": "Hipodonti (Eksik Diş)",
}

# Video link mappings (TÜRKÇE anahtarlar)
VIDEO_LINKS = {
    "Diş Taşı (Calculus)": "https://www.youtube.com/results?search_query=di%C5%9F+ta%C5%9F%C4%B1+nasil+temizlenir",
    "Diş Çürüğü (Karies)": "https://www.youtube.com/results?search_query=di%C5%9F+%C3%A7%C3%BCr%C3%BC%C4%9F%C3%BC+%C3%B6nleme",
    "Diş Eti İltihabı (Gingivitis)": "https://www.youtube.com/results?search_query=di%C5%9F+eti+iltihab%C4%B1+bak%C4%B1m%C4%B1",
    "Aft (Ağız Yarası)": "https://www.youtube.com/results?search_query=aft+nedir+tedavisi",
    "Diş Renklenmesi": "https://www.youtube.com/results?search_query=di%C5%9F+renklenmesi+nas%C4%B1l+ge%C3%A7er",
    "Hipodonti (Eksik Diş)": "https://www.youtube.com/results?search_query=hipodonti+nedir+tedavisi",
}

class DentalImageAnalyzer:
    def __init__(self):
        self.image_processor = None
        self.image_classifier = None
        self.gemini_model = None
        self.memory = ConversationBufferMemory(return_messages=True) if LANGCHAIN_AVAILABLE else None
        
        # Load image classifier
        try:
            self.image_classifier = SiglipForImageClassification.from_pretrained(CLASSIFIER_MODEL_NAME)
            self.image_processor = AutoImageProcessor.from_pretrained(CLASSIFIER_MODEL_NAME)
            print(f"Image classifier loaded: {CLASSIFIER_MODEL_NAME}")
        except Exception as e:
            print(f"Image classifier load error: {e}")
        
        # Load Gemini for NLG
        if genai is not None:
            try:
                gemini_api_key = os.getenv("GEMINI_API_KEY")
                if gemini_api_key:
                    genai.configure(api_key=gemini_api_key)
                    self.gemini_model = genai.GenerativeModel("gemini-1.5-flash")
                    print("Gemini NLG enabled: gemini-1.5-flash")
                else:
                    print("GEMINI_API_KEY not set; using rule-based summary")
            except Exception as e:
                print(f"Gemini init error: {e}")
    
    def analyze_image(self, image_b64: str, user_id: Optional[str] = None) -> Dict:
        """
        Analyze dental image and return classification results with Turkish summary.
        
        Args:
            image_b64: Base64 encoded image (with or without data: prefix)
            user_id: Optional user ID for memory personalization
            
        Returns:
            dict: Analysis results with predictions, summary, weekly plan, and video
        """
        try:
            if self.image_processor is None or self.image_classifier is None:
                return {"error": "Görüntü sınıflandırma modeli yüklenemedi.", "success": False}
            
            # Base64 decode and preprocess
            data_part = image_b64.split(',')[1] if ',' in image_b64 else image_b64
            image_data = base64.b64decode(data_part)
            image = Image.open(io.BytesIO(image_data)).convert("RGB")
            
            inputs = self.image_processor(images=image, return_tensors="pt")
            with torch.no_grad():
                outputs = self.image_classifier(**inputs)
                logits = outputs.logits
                probs = torch.nn.functional.softmax(logits, dim=1).squeeze().tolist()
            
            # Filter <1% probabilities and get top predictions
            filtered_probs = {k: v for k, v in zip(ID_TO_LABEL.values(), probs) if v >= 0.01}
            topk = sorted(filtered_probs.items(), key=lambda x: x[1], reverse=True)[:3]
            topk_labels = [f"{label}: {prob*100:.1f}%" for label, prob in topk]
            raw_results = {label: round(prob * 100, 2) for label, prob in filtered_probs.items()}
            
            # Generate summary and weekly plan
            findings_text = ", ".join(topk_labels)
            summary_data = self._generate_enhanced_summary(findings_text, topk[0][0] if topk else None, user_id)
            
            # Save to memory
            if self.memory and user_id:
                self.memory.chat_memory.add_user_message(f"User {user_id} scan: {findings_text}")
                self.memory.chat_memory.add_ai_message(f"Plan for {topk[0][0] if topk else 'healthy'}.")
            
            return {
                "top_predictions": topk_labels,  # Etiketler Türkçe
                "all_predictions": raw_results,  # Etiketler Türkçe
                "dental_comment": summary_data["comment"],
                "weekly_plan": summary_data["plan"],
                "video_suggestion": VIDEO_LINKS.get(topk[0][0], "https://www.youtube.com/watch?v=general_dental_care") if topk else None,
                "success": True
            }
            
        except Exception as e:
            return {"error": f"Analiz hatası: {str(e)}", "success": False}
    
    def _generate_enhanced_summary(self, findings_text: str, top_issue: Optional[str], user_id: Optional[str]) -> Dict:
        """Generate Turkish summary with weekly plan using Gemini or rule-based, more detailed and personalized."""
        if self.gemini_model is not None:
            try:
                history = self.memory.load_memory_variables({})["history"] if self.memory and user_id else ""
                history_str = f"Önceki: {history[-1] if history else 'Yeni kullanıcı'}." if history else ""
                
                prompt = (
                    "Sen kişisel diş koçu ajansın. Tarama sonuçlarına göre haftalık bakım planı üret. "
                    f"{history_str}\n"
                    "En yüksek olasılıklı sorunu odak yap (%1 üstü). Kısa, motive edici, TÜRKÇE.\n\n"
                    f"Sonuçlar: {findings_text}. Ana sorun: {top_issue or 'Sağlıklı'}.\n\n"
                    "Çıktı JSON: {'comment': 'Özet + riskler + öneriler', 'plan': [{'day': 'Pazartesi', 'task': 'Görev'} x7]}"
                )
                
                gemini_resp = self.gemini_model.generate_content(prompt)
                response_text = getattr(gemini_resp, "text", "")
                try:
                    json_start = response_text.find('{')
                    json_end = response_text.rfind('}') + 1
                    parsed = json.loads(response_text[json_start:json_end])
                    return parsed
                except json.JSONDecodeError:
                    pass
            except Exception as e:
                print(f"Gemini error: {e}")

        # Rule-based fallback, now more detailed and personalized
        base_comment = self._generate_detailed_comment(findings_text, top_issue)
        plan = self._generate_personalized_plan(top_issue)
        return {"comment": base_comment, "plan": plan}

    def _generate_detailed_comment(self, findings_text: str, top_issue: Optional[str]) -> str:
        """Sınıflandırma sonucuna göre detaylı analiz ve öneri döndürür."""
        explanations = {
            "Calculus": "Diş taşları (Calculus) dişler üzerinde birikmiş sertleşmiş plaklardır. Diş taşları diş eti hastalıklarına yol açabilir. Düzenli profesyonel temizlik ve ağız hijyeni çok önemlidir.",
            "Caries": "Diş çürüğü (karies), diş minesinin asitler nedeniyle zarar görmesiyle oluşur. Şekerli gıdalardan kaçınmak, düzenli fırçalama ve diş ipi kullanımı çok önemlidir. Erken tedavi edilmezse ağrı ve enfeksiyona yol açabilir.",
            "Gingivitis": "Diş eti iltihabı (gingivitis), diş etlerinde kızarıklık, şişlik ve kanamaya yol açar. Düzenli ve doğru fırçalama, diş ipi kullanımı ve diş taşı temizliği gereklidir.",
            "Mouth Ulcer": "Aft veya ağız yarası, genellikle stres, vitamin eksikliği veya travma sonrası oluşur. Tuzlu suyla gargara ve yumuşak besinler önerilir.",
            "Tooth Discoloration": "Dişlerde renk değişimi, çay, kahve, sigara gibi dış etkenlerden veya yapısal nedenlerden kaynaklanabilir. Beyazlatıcı macunlar ve profesyonel temizlik yardımcı olabilir.",
            "Hypodontia": "Hipodonti, doğuştan bir veya daha fazla dişin eksik olmasıdır. Ortodontik ve protetik tedavi seçenekleri için diş hekiminize başvurun.",
        }
        risk_level = {
            "Calculus": "Diş eti hastalığı riski artmış.",
            "Caries": "Çürük gelişme riski yüksek.",
            "Gingivitis": "Diş eti kanaması ve çekilmesi riski var.",
            "Mouth Ulcer": "Ağız içi hassasiyeti artmış.",
            "Tooth Discoloration": "Estetik kaygı riski.",
            "Hypodontia": "Çiğneme fonksiyonunda azalma riski.",
        }
        suggestion = {
            "Calculus": "Diş hekiminizde profesyonel temizlik yaptırın. Günde 2 kez fırçalama ve diş ipi kullanımı ihmal etmeyin.",
            "Caries": "Şekerli yiyecek ve içecekleri azaltın, florürlü diş macunu kullanın. Düzenli diş kontrolü yaptırın.",
            "Gingivitis": "Diş eti kanaması varsa daha yumuşak fırça kullanın ve diş ipiyle nazik olun. C vitamini alımınızı artırın.",
            "Mouth Ulcer": "Asitli, baharatlı yiyeceklerden kaçının. Gargara ve lokal kremler kullanabilirsiniz.",
            "Tooth Discoloration": "Renklenmeye neden olan içecekleri azaltın. Diş hekiminizden beyazlatma seçeneklerini öğrenin.",
            "Hypodontia": "Ortodontik muayene önerilir. Eksik dişlerin yerine uygun tedavi planı oluşturulmalı.",
        }
        if top_issue in explanations:
            return (
                f"Durum: {top_issue}\n"
                f"Açıklama: {explanations[top_issue]}\n"
                f"Risk: {risk_level[top_issue]}\n"
                f"Öneri: {suggestion[top_issue]}\n"
                f"Detaylı Sonuçlar: {findings_text}"
            )
        else:
            return f"Dişlerin genel olarak sağlıklı görünüyor. Yine de düzenli bakım ve kontrolleri ihmal etme. Detaylı Sonuçlar: {findings_text}"

    def _generate_personalized_plan(self, top_issue: Optional[str]) -> list:
        """Her güne özel, mantıklı ve çeşitli görevler içeren haftalık plan döndürür."""
        day_tasks = {
            "Calculus": [
                "Diş hekiminizde profesyonel temizlik randevusu alın.",
                "Diş aralarını diş ipiyle temizleyin.",
                "Elektrikli diş fırçası kullanmayı deneyin.",
                "Ağız gargarası ile ağız bakımınızı destekleyin.",
                "Şekerli ve yapışkan gıdalardan uzak durun.",
                "Diş eti masajı yapın.",
                "Diş fırçanızı 3 ayda bir yenileyin."
            ],
            "Caries": [
                "Sabah ve akşam 2 dakika boyunca dişlerinizi florürlü diş macunuyla fırçalayın.",
                "Diş ipi kullanarak arayüz temizliği yapın.",
                "Şekerli yiyecek ve içeceklerden uzak durun.",
                "Günde en az bir kez su için, ağız içini temiz tutun.",
                "Diş hekiminizden çürük kontrolü randevusu alın.",
                "Sağlıklı atıştırmalıklar (meyve, sebze) tercih edin.",
                "Diş fırçanızı düzenli olarak yenileyin."
            ],
            "Gingivitis": [
                "Diş etlerinizi yumuşak fırça ile nazikçe fırçalayın.",
                "Diş ipi ile diş aralarını temizleyin.",
                "Ağız gargarası kullanın.",
                "C vitamini açısından zengin besinler tüketin.",
                "Diş eti masajı yapın.",
                "Diş taşı temizliği için randevu alın.",
                "Diş eti kanaması devam ederse hekime başvurun."
            ],
            "Mouth Ulcer": [
                "Tuzlu suyla gargara yapın.",
                "Baharatlı ve asitli yiyeceklerden kaçının.",
                "Ağız içi hijyenine dikkat edin.",
                "Yumuşak ve soğuk yiyecekler tüketin.",
                "Stresten uzak durmaya çalışın.",
                "Vitamin desteği alın (özellikle B12, C).",
                "Ağız yarası geçmezse hekime başvurun."
            ],
            "Tooth Discoloration": [
                "Çay, kahve ve sigara tüketimini azaltın.",
                "Beyazlatıcı diş macunu kullanın.",
                "Diş hekiminizden profesyonel temizlik randevusu alın.",
                "Bol su için, ağız içini temiz tutun.",
                "Renklenmeye neden olan gıdalardan uzak durun.",
                "Diş fırçanızı düzenli olarak yenileyin.",
                "Beyazlatma işlemleri hakkında hekiminize danışın."
            ],
            "Hypodontia": [
                "Ortodontik muayene için randevu alın.",
                "Eksik dişlerin yerine uygun tedavi seçeneklerini araştırın.",
                "Diş hekiminizle tedavi planınızı görüşün.",
                "Ağız hijyenine ekstra özen gösterin.",
                "Düzenli diş kontrolü yaptırın.",
                "Yumuşak kıllı diş fırçası kullanın.",
                "Beslenme düzeninize dikkat edin."
            ],
        }
        days = ["Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"]
        if top_issue in day_tasks:
            plan = []
            for i, day in enumerate(days):
                plan.append({"day": day, "task": day_tasks[top_issue][i]})
            return plan
        else:
            # Genel bakım planı
            generic_tasks = [
                "Sabah ve akşam dişlerinizi fırçalayın.",
                "Diş ipi kullanın.",
                "Ağız gargarası ile bakım yapın.",
                "Düzenli su tüketin.",
                "Diş hekiminizden kontrol randevusu alın.",
                "Sağlıklı atıştırmalıklar tüketin.",
                "Diş fırçanızı düzenli olarak değiştirin."
            ]
            return [{"day": day, "task": generic_tasks[i]} for i, day in enumerate(days)]

    
    def _generate_rule_based_plan(self, top_issue: Optional[str]) -> List[Dict]:
        """Generate 7-day plan based on top issue."""
        base_tasks = ["2 dk yumuşak fırça ile fırçala", "Diş ipi kullan", "Ağız gargarası"]
        issue_specific = {
            "Calculus": "Plak odaklı: Diş arası temizle",
            "Caries": "Şeker azalt, florürlü macun",
            "Gingivitis": "Diş eti masajı yap",
            "Mouth Ulcer": "Tuzlu su gargara",
            "Tooth Discoloration": "Beyazlatıcı diş macunu",
            "Hypodontia": "Ortodonti danış",
        }.get(top_issue, "Genel bakım uygula")
        
        plan = []
        for i, day in enumerate(["Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"]):
            task = f"{base_tasks[i % len(base_tasks)]}, {issue_specific}"
            plan.append({"day": day, "task": task})
        return plan
    
    def _handle_symptom(self, symptom: str, top_issue: str) -> str:
        """Handle symptoms like 'Ağrım var'."""
        symptom_lower = symptom.lower()
        if "ağrı" in symptom_lower:
            return f"Ağrı: {top_issue} ile ilgili olabilir (zonklama mı?). Ibuprofen al, soğuk kompres uygula. Devam ederse dişçi!"
        elif "kanama" in symptom_lower:
            return "Kanama: Diş eti sorunu? Hafif fırçala, C vitamini artır. 2 günde geçmezse muayene."
        else:
            return "Semptom detaylandır: Ağrı tipi, şişlik? Ek bilgi ver."
    
    def get_agent_tool(self):
        """Returns LangChain Tool for agent integration."""
        if not LANGCHAIN_AVAILABLE:
            raise ImportError("LangChain required.")
        
        def tool_func(image_b64: str, symptom: Optional[str] = None, user_id: Optional[str] = None) -> str:
            analysis = self.analyze_image(image_b64, user_id)
            if not analysis["success"]:
                return analysis["error"]
            
            top_issue = analysis["top_predictions"][0].split(":")[0].strip() if analysis["top_predictions"] else "healthy"
            
            if symptom:
                symptom_advice = self._handle_symptom(symptom, top_issue)
                return f"{analysis['dental_comment']}\nSemptom: {symptom_advice}\nPlan: {json.dumps(analysis['weekly_plan'], ensure_ascii=False, indent=2)}"
            
            return (f"Analiz: {analysis['dental_comment']}\n"
                    f"Video: {analysis['video_suggestion']}\n"
                    f"Plan: {json.dumps(analysis['weekly_plan'], ensure_ascii=False, indent=2)}")
        
        return Tool(
            name="DentalScanAnalyzer",
            func=tool_func,
            description="Analyzes dental image (base64), optional symptom, generates plan and video."
        )

# Global analyzer instance
analyzer = DentalImageAnalyzer()

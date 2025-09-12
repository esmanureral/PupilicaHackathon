from fastapi import FastAPI, Form
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import base64
import io
import os
import torch
from transformers import AutoImageProcessor, SiglipForImageClassification
try:
    # Opsiyonel: .env yükleme
    from dotenv import load_dotenv
    load_dotenv()
except Exception:
    pass
try:
    import google.generativeai as genai
except Exception:
    genai = None

app = FastAPI(title="Dental AI Backend (Classifier + Gemini NLG)", version="0.3.0")

# CORS ayarları
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Modelleri yükle (uygulama başlangıcında)
CLASSIFIER_MODEL_NAME = "prithivMLmods/tooth-agenesis-siglip2"

# Label mapping (modelin çıktı sırasına göre)
ID_TO_LABEL = {
    "0": "Calculus",
    "1": "Caries",
    "2": "Gingivitis",
    "3": "Mouth Ulcer",
    "4": "Tooth Discoloration",
    "5": "Hypodontia",
}

image_processor = None
image_classifier = None
gemini_model = None

try:
    image_classifier = SiglipForImageClassification.from_pretrained(CLASSIFIER_MODEL_NAME)
    image_processor = AutoImageProcessor.from_pretrained(CLASSIFIER_MODEL_NAME)
    print(f"Image classifier loaded: {CLASSIFIER_MODEL_NAME}")
except Exception as e:
    print(f"Image classifier load error: {e}")

# Gemini'yi (varsa) yapılandır
if genai is not None:
    try:
        gemini_api_key = os.getenv("GEMINI_API_KEY")
        if gemini_api_key:
            genai.configure(api_key=gemini_api_key)
            gemini_model = genai.GenerativeModel("gemini-1.5-flash")
            print("Gemini NLG enabled: gemini-1.5-flash")
        else:
            print("GEMINI_API_KEY not set; falling back to local T5 NLG")
    except Exception as e:
        print(f"Gemini init error: {e}")

@app.get("/")
def root():
    return {"message": "AI Backend Hazır! Diş sağlığına hoş geldin 🦷"}

@app.get("/health")
def health_check():
    return {"status": "healthy"}

@app.post("/analyze")
async def analyze_image(user_id: str = Form(...), image_b64: str = Form(...)):
    try:
        if image_processor is None or image_classifier is None:
            return {"error": "Görüntü sınıflandırma modeli yüklenemedi."}

        # Base64 decode
        data_part = image_b64.split(',')[1] if ',' in image_b64 else image_b64
        image_data = base64.b64decode(data_part)
        image = Image.open(io.BytesIO(image_data)).convert("RGB")

        # Preprocess ve sınıflandırma
        inputs = image_processor(images=image, return_tensors="pt")
        with torch.no_grad():
            outputs = image_classifier(**inputs)
            logits = outputs.logits
            probs = torch.nn.functional.softmax(logits, dim=1).squeeze().tolist()

        # Top-k etiketleri hazırla (ilk 3)
        topk = sorted([(i, p) for i, p in enumerate(probs)], key=lambda x: x[1], reverse=True)[:3]
        topk_labels = [f"{ID_TO_LABEL.get(str(i), str(i))}: {p*100:.1f}%" for i, p in topk]
        raw_results = {ID_TO_LABEL.get(str(i), str(i)): round(p * 100, 2) for i, p in enumerate(probs)}

        # Metin üretim prompt'u (Türkçe, diş hekimi tarzı)
        findings_text = ", ".join(topk_labels)
        nlg_prompt = (
            "Sen, diş sağlığı analiz sonuçlarını hastaların anlayacağı dilde yorumlayan uzman bir diş hekimisin. "
            "En yüksek olasılıktaki bulguyu ana teşhis olarak belirt, ikinci önemli bulguyu ek bilgi olarak sun. "
            "%1 altındaki olasılıkları yok say. Kısa, sakinleştirici, anlaşılır TÜRKÇE kullan.\n\n"
            f"Sınıflandırma sonuçları (olasılık): {findings_text}.\n\n"
            "1) Bulguların kısa özeti\n"
            "2) Çürük/plak/gingivitis riski ve olası etkiler\n"
            "3) Günlük bakım ve kontrol önerileri\n"
            "4) Uyarı: Kesin tanı için klinik muayene gerekebilir"
        )

        if gemini_model is not None:
            try:
                gemini_resp = gemini_model.generate_content(nlg_prompt)
                dental_comment = getattr(gemini_resp, "text", None) or "Yorum üretilemedi."
            except Exception as e:
                dental_comment = (
                    "Özet: " + findings_text + ". Kısa öneri: günde 2 kez fırçalama, diş ipi, "
                    "düzenli hekim kontrolü. Klinik muayene ile kesin tanı gerekir."
                )
        else:
            dental_comment = (
                "Özet: " + findings_text + ". Kısa öneri: günde 2 kez fırçalama, diş ipi, "
                "düzenli hekim kontrolü. Klinik muayene ile kesin tanı gerekir."
            )

        return {
            "user_id": user_id,
            "dental_comment": dental_comment,
            "top_predictions": topk_labels,
            "all_predictions": raw_results,
            "message": "Analiz tamamlandı!"
        }
    except Exception as e:
        return {"error": f"Analiz hatası: {str(e)}"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
# Gerekli kütüphanelerin ve modüllerin import edilmesi
from fastapi import FastAPI, Form, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
import threading
import asyncio
from concurrent.futures import ThreadPoolExecutor
from image_analyzer import analyzer
from dental_chatbot import chatbot, start_interactive_cli

# FastAPI uygulamasının oluşturulması ve temel konfigürasyonu
app = FastAPI(title="Dental AI Backend (Classifier + Gemini NLG)", version="0.5.0")

# CORS ayarları. 
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  
    allow_credentials=True,
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)

# CPU-yoğun veya I/O-beklemeli işlemleri (görüntü işleme, model çıkarımı) asenkron olarak çalıştırmak için bir iş parçacığı havuzu.
executor = ThreadPoolExecutor(max_workers=4)

@app.get("/")
def root():
    """API'nin ana (root) endpoint'i. Servisin ayakta olup olmadığını kontrol etmek için kullanılır."""
    return {"message": "AI Backend Hazır! Diş sağlığına hoş geldin 🦷"}

@app.post("/chat")
async def chat(message: str = Form(...), session_id: str = Form(...)):
    """Metin tabanlı sohbet için endpoint. Kullanıcıdan bir mesaj ve oturum ID'si alır, chatbot'tan bir yanıt döndürür."""
    try:
        loop = asyncio.get_event_loop()
        reply = await loop.run_in_executor(executor, chatbot.chat, message) 
    except Exception as e:
        print(f"Chat error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail="Sohbet işlemi sırasında bir hata oluştu. Lütfen tekrar deneyin."
        )

@app.get("/chat/start_cli")
def chat_start_cli():
    """Test amaçlı, terminal üzerinden interaktif bir sohbet oturumu başlatan endpoint."""
    try:
        t = threading.Thread(target=start_interactive_cli, daemon=True)
        t.start()
        return {"status": "started"}
    except Exception as e:
        return {"error": str(e)}

@app.post("/analyze")
async def analyze_image(user_id: str = Form(...), image_b64: str = Form(...), symptom: str = Form(None)):
    """Görüntü analizi için ana endpoint. Base64 formatında bir resim alır ve analiz sonuçlarını döndürür."""
    import datetime
    print(f"[LOG] /analyze endpoint called at {datetime.datetime.now()} for user: {user_id}")
    try:
        loop = asyncio.get_event_loop()
        # Görüntü analizi fonksiyonunu ana thread'i bloklamadan çalıştırır.
        result = await loop.run_in_executor(
            executor, 
            analyzer.analyze_image, 
            image_b64,
            user_id,
            symptom  
        )
        
        if not result.get("success", False):
            raise HTTPException(status_code=400, detail=result.get("error", "Analiz sırasında bilinmeyen bir hata oluştu."))
        return result
    except HTTPException:
        raise  
    except Exception as e:
        print(f"[EXCEPTION] analyze_image error: {str(e)}")
        raise HTTPException(status_code=500, detail="Görüntü analizi sırasında sunucu hatası oluştu.")

# Bu blok, dosyanın doğrudan bir betik olarak çalıştırıldığında FastAPI sunucusunu başlatır.
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)             
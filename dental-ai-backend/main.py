from fastapi import FastAPI, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import threading
import asyncio
from concurrent.futures import ThreadPoolExecutor
from image_analyzer import analyzer
from dental_chatbot import chatbot, start_interactive_cli  # Assuming this exists, ignore if not

app = FastAPI(title="Dental AI Backend (Classifier + Gemini NLG)", version="0.5.0")

# CORS ayarları
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Production'da sadece güvenilen domainler eklenmeli
    allow_credentials=True,
    allow_methods=["POST", "GET"],
    allow_headers=["*"],
)

# Thread pool for CPU-bound operations
executor = ThreadPoolExecutor(max_workers=4)

@app.get("/")
def root():
    return {"message": "AI Backend Hazır! Diş sağlığına hoş geldin 🦷"}

@app.post("/chat")
async def chat(message: str = Form(...), session_id: str = Form(...)):
    try:
        loop = asyncio.get_event_loop()
        reply = await loop.run_in_executor(executor, chatbot.chat, message, session_id)
        return {"reply": reply}
    except Exception as e:
        print(f"Chat error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail="Sohbet işlemi sırasında bir hata oluştu. Lütfen tekrar deneyin."
        )

@app.get("/chat/start_cli")
def chat_start_cli():
    try:
        t = threading.Thread(target=start_interactive_cli, daemon=True)
        t.start()
        return {"status": "started"}
    except Exception as e:
        return {"error": str(e)}

@app.post("/analyze")
async def analyze_image(user_id: str = Form(...), image_b64: str = Form(...), symptom: str = Form(None)):
    import datetime
    print(f"[LOG] /analyze endpoint called at {datetime.datetime.now()} for user: {user_id}")
    try:
        loop = asyncio.get_event_loop()
        # Direct call to analyze_image (no LangChain)
        result = await loop.run_in_executor(
            executor, 
            analyzer.analyze_image, 
            image_b64,
            user_id,
            symptom  # Optional symptom
        )
        
        # Result is already a dict, no need for extra JSON parse
        if not result.get("success", False):
            raise HTTPException(status_code=400, detail=result.get("error", "Analiz sırasında bilinmeyen bir hata oluştu."))
        return result
    except HTTPException:
        raise  # Re-raise HTTP exceptions
    except Exception as e:
        print(f"[EXCEPTION] analyze_image error: {str(e)}")
        raise HTTPException(status_code=500, detail="Görüntü analizi sırasında sunucu hatası oluştu.")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
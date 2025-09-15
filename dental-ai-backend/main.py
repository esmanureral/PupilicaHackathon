from fastapi import FastAPI, Form
from fastapi.middleware.cors import CORSMiddleware
from image_analyzer import analyzer
from dental_chatbot import chatbot, start_interactive_cli
import threading

app = FastAPI(title="Dental AI Backend (Classifier + Gemini NLG)", version="0.4.0")

# CORS ayarları
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {"message": "AI Backend Hazır! Diş sağlığına hoş geldin 🦷"}

@app.post("/chat")
async def chat(message: str = Form(...)):
    reply = chatbot.chat(message)
    return {"reply": reply}

@app.get("/chat/start_cli")
def chat_start_cli():
    try:
        t = threading.Thread(target=start_interactive_cli, daemon=True)
        t.start()
        return {"status": "started"}
    except Exception as e:
        return {"error": str(e)}

@app.post("/analyze")
async def analyze_image(user_id: str = Form(...), image_b64: str = Form(...)):
    try:
        # Use the analyzer module
        result = analyzer.analyze_image(image_b64, user_id)
        
        if not result.get("success", False):
            return {"error": result.get("error", "Bilinmeyen hata")}
        
        return {
            "user_id": user_id,
            "dental_comment": result["dental_comment"],
            "top_predictions": result["top_predictions"],
            "all_predictions": result["all_predictions"],
            "weekly_plan": result.get("weekly_plan", []),
            "video_suggestion": result.get("video_suggestion"),
            "message": "Analiz tamamlandı!"
        }
    except Exception as e:
        return {"error": f"Analiz hatası: {str(e)}"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
# Gerekli kütüphanelerin ve ortam değişkenlerinin yüklenmesi
import os
from typing import Optional

from dotenv import load_dotenv
load_dotenv()

import google.generativeai as genai
genai = None


class DentalChatbot:
    """Gemini AI modelini kullanarak diş sağlığı hakkında sohbet eden sınıf."""

    def __init__(self, api_key: Optional[str] = None):
        """Sınıf başlatıldığında API anahtarını ayarlar ve Gemini modelini kurar."""
        self.api_key = api_key or os.getenv("GEMINI_API_KEY")
        self.model = None
        self._setup_gemini()

    def _setup_gemini(self) -> None:
        """Gemini modelini API anahtarı ile yapılandırır ve kullanıma hazırlar."""
        if genai is None:
            print("google-generativeai not installed; chatbot disabled.")
            return
        if not self.api_key:
            print("GEMINI_API_KEY not set; chatbot disabled.")
            return
        try:
            # API anahtarını ayarla ve kullanılacak modeli seç
            genai.configure(api_key=self.api_key)
            self.model = genai.GenerativeModel("gemini-2.5-flash")
        except Exception as e:
            print(f"Gemini init error: {e}")

    def get_system_prompt(self) -> str:
        """Yapay zekanın nasıl davranması gerektiğini belirleyen sistem talimatlarını döndürür."""
        return (
            "Sen Türkiye'deki diş sağlığı konularında uzman bir sanal asistansın.\n\n"
            "KONUŞMA TARZI:\n"
            "- Normal insan gibi konuş, asistan gibi değil\n"
            "- Kısa ve doğal yanıtlar ver\n"
            "- Selamlaşmalara kısa karşılık ver\n"
            "- Sadece diş sağlığı sorularında detaya gir\n"
            "- Gereksiz uzun açıklamalar yapma\n\n"
            "ACİL DURUM YAKLAŞIMI:\n"
            "- Önce acil rahatlama yöntemleri söyle\n"
            "- Sonra doktora gitmesini söyle\n"
            "- Pratik ve hemen uygulanabilir çözümler ver\n\n"
            "ÖNEMLİ KURALLAR:\n"
            "- Tıbbi tavsiye verme, sadece genel bilgi ver\n"
            "- Acil durumlarda mutlaka diş hekimine başvurmasını söyle\n"
            "- Normal insan gibi konuş, resmi olma\n"
            "- Kısa ve samimi yanıtlar ver\n"
            "- Sadece diş sağlığı konularında uzun yanıtlar ver\n"
        )

    def chat(self, user_input: str) -> str:
        if self.model is None:
            return "Üzgünüm, sohbet özelliği şu anda devre dışı. API anahtarı eksik veya servis kullanılamıyor."
        try:
            prompt = f"{self.get_system_prompt()}\n\nKullanıcı: {user_input}"
            response = self.model.generate_content(prompt)
            return (getattr(response, "text", None) or "Üzgünüm, şu an yanıt üretemiyorum.").strip()
        except Exception as e:
            return f"Üzgünüm, bir hata oluştu: {str(e)}"


# Global instance to be imported by FastAPI app
chatbot = DentalChatbot()

def start_interactive_cli() -> None:
    """Genel chatbot nesnesini kullanarak interaktif bir komut satırı sohbet döngüsü başlatır."""
    print("\n🦷 Diş Sağlığı Chatbot’u (çıkmak için: çık/exit/quit)\n")
    try:
        while True:
            msg = input("Sen: ").strip()
            if msg.lower() in ("çık", "exit", "quit"):
                print("Görüşürüz! 😊")
                break
            if not msg:
                continue
            print("Bot:", chatbot.chat(msg), "\n")
    except KeyboardInterrupt:
        print("\nGörüşürüz! 😊")
                                            
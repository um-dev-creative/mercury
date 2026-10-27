package com.umdc.mercury.telegram;

/** The bot's own replies to commands (T-114), in Spanish and English. Notification texts are templates (T-116). */
final class TelegramBotTexts {

    private TelegramBotTexts() { }

    static String lang(String language) {
        return language != null && language.toLowerCase(java.util.Locale.ROOT).startsWith("en") ? "en" : "es";
    }

    static String linked(String l) {
        return "en".equals(l) ? "✅ Done! Your account is linked. You will get your ZeroBound alerts here. Send /unlink or /desvincular to stop."
                : "✅ ¡Listo! Tu cuenta quedó vinculada. Recibirás aquí tus avisos de ZeroBound. Envía /desvincular para dejar de recibirlos.";
    }

    static String invalidToken(String l) {
        return "en".equals(l) ? "This link is invalid or has expired. Open ZeroBound, go to Notifications and connect Telegram again."
                : "Este enlace no es válido o caducó. Abre ZeroBound, ve a Notificaciones y vuelve a conectar Telegram.";
    }

    static String howToLink(String l) {
        return "en".equals(l) ? "To link your account, open ZeroBound, go to Notifications and press Connect Telegram."
                : "Para vincular tu cuenta, abre ZeroBound, ve a Notificaciones y pulsa Conectar Telegram.";
    }

    static String help(String l) {
        return "en".equals(l) ? "I send your ZeroBound alerts.\n/status – is this chat linked?\n/desvincular – stop receiving alerts\n/help – this message"
                : "Envío tus avisos de ZeroBound.\n/estado – ¿este chat está vinculado?\n/desvincular – dejar de recibir avisos\n/ayuda – este mensaje";
    }

    static String statusLinked(String l) {
        return "en".equals(l) ? "This chat is linked to your ZeroBound account." : "Este chat está vinculado a tu cuenta de ZeroBound.";
    }

    static String statusBlocked(String l) {
        return "en".equals(l) ? "Alerts to this chat are paused. Connect Telegram again from ZeroBound."
                : "Los avisos a este chat están en pausa. Vuelve a conectar Telegram desde ZeroBound.";
    }

    static String statusNone(String l) {
        return howToLink(l);
    }

    static String unlinked(String l) {
        return "en".equals(l) ? "Unlinked. You will not get more alerts here." : "Desvinculado. Ya no recibirás avisos aquí.";
    }

    static String replaced(String l) {
        return "en".equals(l) ? "Your account was linked to another Telegram chat, so this chat is unlinked."
                : "Tu cuenta se vinculó a otro chat de Telegram, así que este chat quedó desvinculado.";
    }
}

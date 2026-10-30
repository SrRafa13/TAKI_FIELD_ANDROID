package br.com.takirastreadores.field;

import android.content.SharedPreferences;
import android.graphics.Color;
import java.util.*;

final class ThemeEngine {
    static final class Theme {
        final String key, name, emoji;
        final int bg, surface, surface2, text, muted, stroke, accent, accent2;
        final boolean light;
        Theme(String key, String name, String emoji, boolean light, int bg, int surface, int surface2, int text, int muted, int stroke, int accent, int accent2) {
            this.key = key; this.name = name; this.emoji = emoji; this.light = light;
            this.bg = bg; this.surface = surface; this.surface2 = surface2; this.text = text; this.muted = muted; this.stroke = stroke; this.accent = accent; this.accent2 = accent2;
        }
    }

    static final String DARK = "dark";
    static final String LIGHT = "light";
    static final String CHRISTMAS = "christmas";
    static final String NEW_YEAR = "new_year";
    static final String CARNIVAL = "carnival";
    static final String EASTER = "easter";
    static final String HALLOWEEN = "halloween";

    private static final LinkedHashMap<String, Theme> THEMES = new LinkedHashMap<>();
    static {
        add(new Theme(DARK, "TAKI Dark", "🌑", false,
                rgb(7,8,10), rgb(18,19,23), rgb(28,29,34), rgb(246,247,249), rgb(155,158,166), rgb(48,50,58), rgb(238,35,48), rgb(190,22,35)));
        add(new Theme(LIGHT, "TAKI Light", "☀️", true,
                rgb(244,245,247), Color.WHITE, rgb(236,238,242), rgb(25,27,31), rgb(103,107,116), rgb(214,217,223), rgb(224,31,48), rgb(183,22,36)));
        add(new Theme(CHRISTMAS, "Natal", "🎄", false,
                rgb(8,18,14), rgb(17,33,26), rgb(24,47,36), rgb(248,249,245), rgb(174,190,180), rgb(51,82,65), rgb(220,38,54), rgb(31,126,78)));
        add(new Theme(NEW_YEAR, "Ano Novo", "🎆", false,
                rgb(8,10,18), rgb(20,23,38), rgb(31,35,53), rgb(250,249,244), rgb(180,181,195), rgb(62,64,84), rgb(227,180,67), rgb(120,96,230)));
        add(new Theme(CARNIVAL, "Carnaval", "🎭", false,
                rgb(18,8,24), rgb(34,18,44), rgb(51,27,64), rgb(255,249,255), rgb(203,176,211), rgb(81,48,94), rgb(237,43,122), rgb(62,207,170)));
        add(new Theme(EASTER, "Páscoa", "🐰", true,
                rgb(249,242,247), rgb(255,251,254), rgb(242,228,239), rgb(53,39,51), rgb(117,92,111), rgb(221,197,216), rgb(173,85,146), rgb(111,151,94)));
        add(new Theme(HALLOWEEN, "Halloween", "🎃", false,
                rgb(12,8,15), rgb(27,18,31), rgb(40,25,44), rgb(249,244,250), rgb(179,159,184), rgb(70,48,75), rgb(242,111,33), rgb(124,72,173)));

        // Extras deliberadamente não documentados na interface externa do projeto.
        add(new Theme("junina", "Arraiá", "🌽", false,
                rgb(18,13,8), rgb(39,28,18), rgb(55,38,23), rgb(255,248,235), rgb(205,181,146), rgb(86,61,35), rgb(232,118,31), rgb(220,177,47)));
        add(new Theme("aurora", "Aurora", "🌌", false,
                rgb(6,12,20), rgb(14,27,37), rgb(21,42,53), rgb(239,252,249), rgb(151,189,188), rgb(42,75,82), rgb(55,219,174), rgb(90,105,238)));
        add(new Theme("circuit", "Circuito", "⚡", false,
                rgb(5,10,8), rgb(13,25,20), rgb(19,39,30), rgb(238,255,245), rgb(142,187,161), rgb(39,75,57), rgb(49,224,122), rgb(225,211,47)));
        add(new Theme("sunset", "Pôr do Sol", "🌇", true,
                rgb(252,242,235), rgb(255,249,245), rgb(247,226,213), rgb(58,38,34), rgb(125,88,78), rgb(225,194,181), rgb(224,88,61), rgb(123,78,169)));
    }

    private static int rgb(int r,int g,int b){ return Color.rgb(r,g,b); }
    private static void add(Theme t){ THEMES.put(t.key, t); }

    static ArrayList<Theme> all(){ return new ArrayList<>(THEMES.values()); }
    static Theme byKey(String key){ Theme t = THEMES.get(key); return t == null ? THEMES.get(DARK) : t; }

    static Theme current(SharedPreferences p){ return byKey(effectiveKey(p)); }

    static String effectiveKey(SharedPreferences p){
        String preview = p.getString("theme_preview", "").trim();
        if (!preview.isEmpty() && THEMES.containsKey(preview)) return preview;
        if (p.getBoolean("seasonal_auto", true)) {
            String auto = seasonalKey(Calendar.getInstance());
            if (auto != null) return auto;
        }
        String selected = p.getString("theme_selected", "").trim();
        if (!selected.isEmpty() && THEMES.containsKey(selected)) return selected;
        String legacy = p.getString("theme_mode", DARK);
        return LIGHT.equals(legacy) ? LIGHT : DARK;
    }

    static String seasonalKey(Calendar now){
        int year = now.get(Calendar.YEAR);
        Calendar day = dateOnly(now);

        if (between(day, date(year, Calendar.DECEMBER, 29), date(year, Calendar.DECEMBER, 31)) ||
                between(day, date(year, Calendar.JANUARY, 1), date(year, Calendar.JANUARY, 2))) return NEW_YEAR;
        if (between(day, date(year, Calendar.DECEMBER, 10), date(year, Calendar.DECEMBER, 28))) return CHRISTMAS;
        if (between(day, date(year, Calendar.OCTOBER, 28), date(year, Calendar.OCTOBER, 31))) return HALLOWEEN;

        Calendar easter = easter(year);
        Calendar easterStart = addDays(easter, -3);
        if (between(day, easterStart, easter)) return EASTER;
        Calendar carnivalStart = addDays(easter, -50);
        Calendar carnivalEnd = addDays(easter, -47);
        if (between(day, carnivalStart, carnivalEnd)) return CARNIVAL;

        // Tema extra sazonal mantido como descoberta dentro do app.
        if (between(day, date(year, Calendar.JUNE, 13), date(year, Calendar.JUNE, 30))) return "junina";
        return null;
    }

    static String nextPublicSeasonalLabel(){
        Calendar now = dateOnly(Calendar.getInstance());
        int year = now.get(Calendar.YEAR);
        ArrayList<Event> events = publicEvents(year);
        events.addAll(publicEvents(year + 1));
        Collections.sort(events, Comparator.comparingLong(a -> a.start.getTimeInMillis()));
        for (Event e : events) if (!e.end.before(now)) {
            if (!now.before(e.start) && !now.after(e.end)) return "Ativo agora: " + e.emoji + " " + e.name;
            return "Próximo: " + e.emoji + " " + e.name + " • " + String.format(Locale.getDefault(), "%02d/%02d", e.start.get(Calendar.DAY_OF_MONTH), e.start.get(Calendar.MONTH)+1);
        }
        return "Temas sazonais automáticos";
    }

    static String decor(String key){
        if (CHRISTMAS.equals(key)) return "❄  ✦  🎄  ✦  ❄";
        if (NEW_YEAR.equals(key)) return "✦  ✨  🎆  ✨  ✦";
        if (CARNIVAL.equals(key)) return "◆  🎭  ✦  ◇  🎉";
        if (EASTER.equals(key)) return "✿  🐰  ✿  🥚  ✿";
        if (HALLOWEEN.equals(key)) return "☾  🎃  ✦  👻  ☾";
        if ("junina".equals(key)) return "✦  🌽  🔥  🎏  ✦";
        if ("aurora".equals(key)) return "✦  ⋆  🌌  ⋆  ✦";
        if ("circuit".equals(key)) return "⌁  ⚡  ⌁  ▣  ⌁";
        if ("sunset".equals(key)) return "☀  ◇  🌇  ◇  ☀";
        return "";
    }

    private static ArrayList<Event> publicEvents(int year){
        ArrayList<Event> x = new ArrayList<>();
        x.add(new Event("Ano Novo","🎆",date(year,Calendar.JANUARY,1),date(year,Calendar.JANUARY,2)));
        Calendar easter = easter(year);
        x.add(new Event("Carnaval","🎭",addDays(easter,-50),addDays(easter,-47)));
        x.add(new Event("Páscoa","🐰",addDays(easter,-3),easter));
        x.add(new Event("Halloween","🎃",date(year,Calendar.OCTOBER,28),date(year,Calendar.OCTOBER,31)));
        x.add(new Event("Natal","🎄",date(year,Calendar.DECEMBER,10),date(year,Calendar.DECEMBER,28)));
        x.add(new Event("Ano Novo","🎆",date(year,Calendar.DECEMBER,29),date(year,Calendar.DECEMBER,31)));
        return x;
    }

    private static final class Event {
        final String name, emoji; final Calendar start, end;
        Event(String n,String e,Calendar s,Calendar f){name=n;emoji=e;start=s;end=f;}
    }

    private static Calendar easter(int y){
        int a = y % 19;
        int b = y / 100;
        int c = y % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return date(y, month - 1, day);
    }

    private static Calendar date(int y,int m,int d){ Calendar c = Calendar.getInstance(); c.clear(); c.set(y,m,d,12,0,0); return c; }
    private static Calendar dateOnly(Calendar in){ return date(in.get(Calendar.YEAR), in.get(Calendar.MONTH), in.get(Calendar.DAY_OF_MONTH)); }
    private static Calendar addDays(Calendar c,int n){ Calendar x=(Calendar)c.clone(); x.add(Calendar.DAY_OF_MONTH,n); return x; }
    private static boolean between(Calendar x,Calendar a,Calendar b){ return !x.before(a) && !x.after(b); }

    private ThemeEngine(){}
}

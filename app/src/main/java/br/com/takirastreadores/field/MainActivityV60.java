package br.com.takirastreadores.field;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivityV60 extends MainActivityV50 {
    static final String VERSION_60 = "6.0.0";
    String currentSection = "CENTRAL";

    @Override public void onCreate(Bundle b) {
        String incoming = incomingText(getIntent());
        super.onCreate(b);
        if (incoming != null && !incoming.trim().isEmpty()) {
            prefs.edit().putString("pending_shared_location", incoming.trim()).apply();
            if (!prefs.getString("technician", "").isEmpty()) gpsScreen();
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String incoming = incomingText(intent);
        if (incoming != null && !incoming.trim().isEmpty()) {
            prefs.edit().putString("pending_shared_location", incoming.trim()).apply();
            if (!prefs.getString("technician", "").isEmpty()) gpsScreen();
        }
    }

    String incomingText(Intent i) {
        if (i == null) return null;
        if (Intent.ACTION_SEND.equals(i.getAction()) && "text/plain".equals(i.getType())) {
            CharSequence s = i.getCharSequenceExtra(Intent.EXTRA_TEXT);
            return s == null ? null : s.toString();
        }
        return null;
    }

    @Override void home() {
        if (prefs.getString("technician", "").trim().isEmpty()) {
            welcomeScreen();
            return;
        }
        centralScreen();
    }

    void welcomeScreen() {
        currentSection = "WELCOME";
        list.removeAllViews();
        applyBackground();
        Space top = new Space(this); list.addView(top, new LinearLayout.LayoutParams(1, dp(48)));
        TextView mark = txt("●", 15, RED, true); mark.setGravity(Gravity.CENTER); list.addView(mark, mb(10));
        TextView brand = txt("TAKI FIELD", 30, themeText(), true); brand.setGravity(Gravity.CENTER); list.addView(brand, mb(4));
        TextView version = txt("FIELD OPERATIONS • 6.0", 11, RED, true); version.setGravity(Gravity.CENTER); list.addView(version, mb(28));

        LinearLayout card = card();
        TextView hi = txt("BEM-VINDO", 22, themeText(), true); hi.setGravity(Gravity.CENTER); card.addView(hi, mb(5));
        TextView sub = txt("Selecione quem está usando o aplicativo hoje.", 13, themeMuted(), false); sub.setGravity(Gravity.CENTER); card.addView(sub, mb(18));
        card.addView(techButton("EDUARDO"), new LinearLayout.LayoutParams(-1, dp(56)));
        Space g = new Space(this); card.addView(g, new LinearLayout.LayoutParams(1, dp(10)));
        card.addView(techButton("RAFAEL"), new LinearLayout.LayoutParams(-1, dp(56)));
        list.addView(card, mb(18));
        TextView local = txt("Perfil salvo somente neste aparelho • funciona offline", 11, themeMuted(), false); local.setGravity(Gravity.CENTER); list.addView(local);
        animateIn(card, 80);
    }

    Button techButton(String name) {
        return button("◎   " + name, false, v -> {
            prefs.edit().putString("technician", name.substring(0,1) + name.substring(1).toLowerCase(Locale.ROOT)).apply();
            String pending = prefs.getString("pending_shared_location", "");
            if (!pending.isEmpty()) gpsScreen(); else centralScreen();
        });
    }

    void centralScreen() {
        screenHeader("CENTRAL TÉCNICA", "Operação de campo, diagnóstico e documentação.", "CENTRAL");

        File draft = new File(getFilesDir(), "installation_batch_v5.json");
        if (draft.exists()) {
            LinearLayout resume = card();
            LinearLayout r = row();
            TextView icon = icon("↻"); r.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
            LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL);
            t.addView(txt("Lote salvo no aparelho", 14, themeText(), true));
            t.addView(txt("Continue de onde o atendimento parou.", 12, themeMuted(), false));
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(button("RETOMAR", true, v -> openInstallations()), new LinearLayout.LayoutParams(dp(100), dp(44)));
            resume.addView(r); list.addView(resume, mb(11)); animateIn(resume, 40);
        }

        featureCard("▣", "INSTALAÇÕES", "Individual ou em lote, fotos, PDF e retomada automática.", this::openInstallations, 70);
        featureCard("⌖", "GPS", "Destinos salvos e rota direta pelo Waze ou Google Maps.", this::gpsScreen, 100);
        featureCard("◫", "DASHBOARD", "Resumo mensal, filtros e calendário de serviços.", this::dashboardScreen, 130);
        featureCard("▦", "HISTÓRICO DE SERVIÇOS", "Pesquise placas e veja quem realizou cada atendimento.", this::serviceHistoryScreen, 160);
        featureCard("⚡", "DIAGNÓSTICO GUIADO", "Atalhos para comunicação, GPS, ignição, bloqueio e RS232.", this::diagnosticsScreen, 190);

        LinearLayout mini = card();
        mini.addView(txt("ATALHOS TÉCNICOS", 13, themeMuted(), true), mb(9));
        LinearLayout r1 = row();
        r1.addView(button("FAVORITOS", false, v -> favoritesScreen()), new LinearLayout.LayoutParams(0, dp(46), 1)); gap(r1);
        r1.addView(button("PERFIS", false, v -> profilesScreen()), new LinearLayout.LayoutParams(0, dp(46), 1)); mini.addView(r1, mb(8));
        LinearLayout r2 = row();
        r2.addView(button("INTERPRETAR", false, v -> parserScreen()), new LinearLayout.LayoutParams(0, dp(46), 1)); gap(r2);
        r2.addView(button("LOG COMANDOS", false, v -> historyScreen()), new LinearLayout.LayoutParams(0, dp(46), 1)); mini.addView(r2);
        list.addView(mini, mb(12)); animateIn(mini, 220);

        ArrayList<ServiceStore.Entry> recent = ServiceStore.list(this);
        if (!recent.isEmpty()) {
            LinearLayout rec = card(); rec.addView(txt("ÚLTIMOS SERVIÇOS", 13, themeMuted(), true), mb(8));
            for (int i = 0; i < Math.min(3, recent.size()); i++) {
                ServiceStore.Entry e = recent.get(i);
                TextView x = txt(e.plate + "   •   " + e.service + "   •   " + e.technician, 13, themeText(), i == 0);
                rec.addView(x, mb(i == Math.min(3, recent.size()) - 1 ? 0 : 7));
            }
            list.addView(rec, mb(12)); animateIn(rec, 250);
        }
    }

    void openInstallations() { startActivity(new Intent(this, InstallationActivityV60.class)); }

    void featureCard(String symbol, String title, String desc, Runnable action, long delay) {
        LinearLayout c = card(); c.setClickable(true); c.setFocusable(true); c.setOnClickListener(v -> action.run());
        LinearLayout r = row(); TextView i = icon(symbol); r.addView(i, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout text = new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL); text.setPadding(dp(4), 0, 0, 0);
        text.addView(txt(title, 15, themeText(), true)); text.addView(txt(desc, 12, themeMuted(), false));
        r.addView(text, new LinearLayout.LayoutParams(0, -2, 1)); TextView go = txt("›", 28, themeMuted(), false); go.setGravity(Gravity.CENTER); r.addView(go, new LinearLayout.LayoutParams(dp(30), dp(48))); c.addView(r);
        list.addView(c, mb(10)); animateIn(c, delay);
    }

    TextView icon(String s) {
        TextView v = txt(s, 20, RED, true); v.setGravity(Gravity.CENTER); v.setBackground(round(themeSurface2(), 14, themeStroke(), 1)); return v;
    }

    void commandsScreen() {
        screenHeader("COMANDOS", "Catálogo técnico por fabricante e equipamento.", "COMANDOS");
        LinearLayout selector = card();
        selector.addView(label("FABRICANTE"));
        vendor = spinner(new String[]{"Suntech", "X3Tech", "Queclink", RS232_VENDOR, QUANTA_VENDOR}); selector.addView(vendor, mb(10));
        selector.addView(label("MODELO")); model = spinner(models("Suntech")); selector.addView(model, mb(10));
        idField = input("Número do módulo / ESN / IMEI"); idField.setText(prefs.getString("id", "")); selector.addView(idField, mb(8));
        phoneField = input("Número do chip (opcional)"); phoneField.setInputType(3); phoneField.setText(prefs.getString("phone", "")); selector.addView(phoneField);
        list.addView(selector, mb(12)); animateIn(selector, 50);

        search = input("Buscar: reset, APN, velocidade..."); list.addView(search, mb(10));
        LinearLayout q1 = row();
        q1.addView(button("CHECKLIST", false, v -> checklist()), new LinearLayout.LayoutParams(0, dp(46), 1)); gap(q1);
        q1.addView(button("LER ETIQUETA", false, v -> scanLabel()), new LinearLayout.LayoutParams(0, dp(46), 1)); list.addView(q1, mb(8));
        LinearLayout q2 = row();
        q2.addView(button("FAVORITOS", false, v -> favoritesScreen()), new LinearLayout.LayoutParams(0, dp(46), 1)); gap(q2);
        q2.addView(button("LIMPAR BUSCA", false, v -> search.setText("")), new LinearLayout.LayoutParams(0, dp(46), 1)); list.addView(q2, mb(12));

        status = txt("", 12, themeMuted(), false); list.addView(status, mb(8));
        LinearLayout commandHolder = new LinearLayout(this); commandHolder.setId(9876); commandHolder.setOrientation(LinearLayout.VERTICAL); list.addView(commandHolder);

        vendor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> p) {}
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String x = vendor.getSelectedItem().toString(); model.setAdapter(adapter(models(x))); render();
            }
        });
        model.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> p) {}
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { render(); }
        });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { render(); }
            public void afterTextChanged(Editable e) {}
        });
        idField.addTextChangedListener(saveWatcher("id"));
        idField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { render(); }
            public void afterTextChanged(Editable e) {}
        });
        phoneField.addTextChangedListener(saveWatcher("phone"));
        render();
    }

    @Override void openCommand(Cmd c) {
        commandsScreen();
        int pos = 0;
        for (int i = 0; i < vendor.getCount(); i++) if (vendor.getItemAtPosition(i).toString().equals(c.vendor)) { pos = i; break; }
        vendor.setSelection(pos); search.setText(c.title);
    }

    @Override void scanLabel() {
        if (!"COMANDOS".equals(currentSection) || idField == null) commandsScreen();
        super.scanLabel();
    }

    void gpsScreen() {
        screenHeader("GPS", "Escolha o navegador, salve clientes e inicie a rota em um toque.", "GPS");
        LinearLayout provider = card(); provider.addView(label("NAVEGADOR PADRÃO"), mb(8));
        LinearLayout pr = row();
        String savedProvider = prefs.getString("gps_provider", "Waze");
        Button waze = navChoice("WAZE", "Waze".equals(savedProvider));
        Button google = navChoice("GOOGLE MAPS", "Google".equals(savedProvider));
        pr.addView(waze, new LinearLayout.LayoutParams(0, dp(48), 1)); gap(pr); pr.addView(google, new LinearLayout.LayoutParams(0, dp(48), 1)); provider.addView(pr);
        list.addView(provider, mb(11)); animateIn(provider, 40);

        LinearLayout dest = card(); dest.addView(txt("NOVO DESTINO", 15, themeText(), true), mb(4));
        dest.addView(txt("Digite endereço, coordenadas ou compartilhe uma localização do Google Maps com o TAKI FIELD.", 12, themeMuted(), false), mb(10));
        EditText destination = input("Rua, número, cidade ou coordenadas");
        String pending = prefs.getString("pending_shared_location", "");
        if (!pending.isEmpty()) {
            destination.setText(extractDestination(pending));
            prefs.edit().remove("pending_shared_location").apply();
        }
        dest.addView(destination, mb(9));
        LinearLayout dr = row();
        dr.addView(button("INICIAR ROTA", true, v -> routeTo(destination.getText().toString())), new LinearLayout.LayoutParams(0, dp(50), 1)); gap(dr);
        dr.addView(button("SALVAR", false, v -> promptSaveDestination(destination.getText().toString())), new LinearLayout.LayoutParams(0, dp(50), 1)); dest.addView(dr, mb(8));
        dest.addView(button("PESQUISAR NO GOOGLE MAPS", false, v -> searchOnMaps(destination.getText().toString())), new LinearLayout.LayoutParams(-1, dp(46)));
        list.addView(dest, mb(12)); animateIn(dest, 70);

        ArrayList<Destination> saved = loadDestinations();
        TextView sh = txt("DESTINOS SALVOS  •  " + saved.size(), 12, themeMuted(), true); list.addView(sh, mb(8));
        if (saved.isEmpty()) {
            LinearLayout empty = card(); empty.addView(txt("Nenhum cliente salvo ainda.", 14, themeText(), true), mb(4)); empty.addView(txt("Digite o endereço acima e toque em SALVAR. Ex.: CLIENTE ABÓBORA.", 12, themeMuted(), false)); list.addView(empty, mb(10));
        } else {
            int delay = 100;
            for (Destination d : saved) {
                LinearLayout c = card();
                LinearLayout rr = row();
                LinearLayout tt = new LinearLayout(this); tt.setOrientation(LinearLayout.VERTICAL); tt.addView(txt(d.name, 15, themeText(), true)); tt.addView(txt(d.value, 12, themeMuted(), false)); rr.addView(tt, new LinearLayout.LayoutParams(0, -2, 1));
                Button go = button("ROTA", true, v -> routeTo(d.value)); rr.addView(go, new LinearLayout.LayoutParams(dp(78), dp(44))); c.addView(rr, mb(8));
                TextView del = txt("remover destino", 11, themeMuted(), false); del.setGravity(Gravity.END); del.setOnClickListener(v -> { deleteDestination(d); gpsScreen(); }); c.addView(del);
                list.addView(c, mb(9)); animateIn(c, delay); delay += 25;
            }
        }

        waze.setOnClickListener(v -> { prefs.edit().putString("gps_provider", "Waze").apply(); gpsScreen(); });
        google.setOnClickListener(v -> { prefs.edit().putString("gps_provider", "Google").apply(); gpsScreen(); });
    }

    Button navChoice(String label, boolean selected) {
        Button b = button(label, selected, null); b.setOnClickListener(null);
        if (!selected) b.setBackground(round(themeSurface2(), 14, themeStroke(), 1));
        return b;
    }

    void promptSaveDestination(String raw) {
        String value = extractDestination(raw);
        if (value.trim().isEmpty()) { Toast.makeText(this, "Informe um destino primeiro", Toast.LENGTH_SHORT).show(); return; }
        final EditText name = input("Ex.: CLIENTE ABÓBORA");
        LinearLayout wrap = new LinearLayout(this); wrap.setPadding(dp(20), dp(8), dp(20), 0); wrap.addView(name, new LinearLayout.LayoutParams(-1, dp(52)));
        new AlertDialog.Builder(this).setTitle("Nome do destino").setView(wrap).setNegativeButton("Cancelar", null).setPositiveButton("SALVAR", (d,w) -> {
            String n = name.getText().toString().trim(); if (n.isEmpty()) n = "CLIENTE " + (loadDestinations().size() + 1);
            ArrayList<Destination> a = loadDestinations(); a.add(new Destination(n, value)); saveDestinations(a); gpsScreen();
        }).show();
    }

    static class Destination {
        String name, value;
        Destination(String n, String v) { name = n; value = v; }
    }

    ArrayList<Destination> loadDestinations() {
        ArrayList<Destination> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString("gps_destinations_v6", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i); if (o != null) out.add(new Destination(o.optString("name"), o.optString("value")));
            }
        } catch (Exception ignored) {}
        return out;
    }

    void saveDestinations(ArrayList<Destination> list) {
        JSONArray a = new JSONArray();
        try { for (Destination d : list) { JSONObject o = new JSONObject(); o.put("name", d.name); o.put("value", d.value); a.put(o); } } catch (Exception ignored) {}
        prefs.edit().putString("gps_destinations_v6", a.toString()).apply();
    }

    void deleteDestination(Destination target) {
        ArrayList<Destination> a = loadDestinations();
        for (int i = a.size() - 1; i >= 0; i--) if (a.get(i).name.equals(target.name) && a.get(i).value.equals(target.value)) a.remove(i);
        saveDestinations(a);
    }

    String extractDestination(String raw) {
        if (raw == null) return "";
        String s = raw.trim(); if (s.isEmpty()) return "";
        Matcher coords = Pattern.compile("(-?\\d{1,2}\\.\\d{3,})\\s*,\\s*(-?\\d{1,3}\\.\\d{3,})").matcher(s);
        if (coords.find()) return coords.group(1) + "," + coords.group(2);
        try {
            Matcher url = Pattern.compile("https?://\\S+").matcher(s);
            if (url.find()) {
                Uri u = Uri.parse(url.group());
                String q = u.getQueryParameter("q"); if (q == null) q = u.getQueryParameter("query"); if (q == null) q = u.getQueryParameter("destination");
                if (q != null && !q.trim().isEmpty()) return q.trim();
            }
        } catch (Exception ignored) {}
        String best = "";
        for (String line : s.split("\\r?\\n")) {
            String x = line.trim(); if (x.isEmpty() || x.startsWith("http://") || x.startsWith("https://")) continue;
            String low = x.toLowerCase(Locale.ROOT); if (low.contains("google maps") || low.equals("compartilhado")) continue;
            best = x;
        }
        if (!best.isEmpty()) return best;
        return s;
    }

    boolean isUrl(String s) { return s != null && (s.startsWith("http://") || s.startsWith("https://")); }

    void routeTo(String raw) {
        String d = extractDestination(raw);
        if (d.isEmpty()) { Toast.makeText(this, "Informe um destino", Toast.LENGTH_SHORT).show(); return; }
        String provider = prefs.getString("gps_provider", "Waze");
        if (isUrl(d)) {
            if ("Google".equals(provider)) { openPreferred(d, "com.google.android.apps.maps"); return; }
            resolveSharedLinkForWaze(d); return;
        }
        if ("Google".equals(provider)) openGoogleRoute(d); else openWazeRoute(d);
    }

    void openGoogleRoute(String d) {
        try { openPreferred("https://www.google.com/maps/dir/?api=1&destination=" + URLEncoder.encode(d, "UTF-8") + "&travelmode=driving&dir_action=navigate", "com.google.android.apps.maps"); }
        catch (Exception e) { Toast.makeText(this, "Não foi possível montar a rota", Toast.LENGTH_SHORT).show(); }
    }

    void openWazeRoute(String d) {
        try { openPreferred("https://waze.com/ul?q=" + URLEncoder.encode(d, "UTF-8") + "&navigate=yes", "com.waze"); }
        catch (Exception e) { Toast.makeText(this, "Não foi possível montar a rota", Toast.LENGTH_SHORT).show(); }
    }

    void openPreferred(String url, String pkg) {
        try { Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url)); i.setPackage(pkg); startActivity(i); }
        catch (Exception e) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception x) { Toast.makeText(this, "Navegador indisponível", Toast.LENGTH_LONG).show(); } }
    }

    void resolveSharedLinkForWaze(String url) {
        Toast.makeText(this, "Convertendo localização compartilhada...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String result = null;
            try {
                HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection(); c.setInstanceFollowRedirects(true); c.setConnectTimeout(5000); c.setReadTimeout(5000); c.setRequestProperty("User-Agent", "TAKI-FIELD/6.0"); c.getInputStream().close();
                String finalUrl = c.getURL().toString(); String parsed = extractDestination(finalUrl); if (!parsed.equals(finalUrl)) result = parsed;
            } catch (Exception ignored) {}
            final String r = result;
            runOnUiThread(() -> {
                if (r != null && !r.isEmpty()) openWazeRoute(r);
                else new AlertDialog.Builder(this).setTitle("Não consegui converter o link").setMessage("Esse link curto do Google não expôs o endereço ou as coordenadas. Abra a localização, copie o endereço ou compartilhe um texto com o endereço junto do link.").setPositiveButton("OK", null).show();
            });
        }).start();
    }

    void searchOnMaps(String raw) {
        String d = extractDestination(raw); if (d.isEmpty()) { Toast.makeText(this, "Digite algo para pesquisar", Toast.LENGTH_SHORT).show(); return; }
        try { openPreferred("https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(d, "UTF-8"), "com.google.android.apps.maps"); }
        catch (Exception e) { Toast.makeText(this, "Não foi possível pesquisar", Toast.LENGTH_SHORT).show(); }
    }

    void serviceHistoryScreen() {
        screenHeader("HISTÓRICO DE SERVIÇOS", "Tudo salvo localmente neste aparelho.", "HISTÓRICO");
        ArrayList<ServiceStore.Entry> entries = ServiceStore.list(this);
        LinearLayout filters = card();
        EditText q = input("Buscar placa, módulo ou técnico"); filters.addView(q, mb(8));
        Spinner service = spinner(new String[]{"Todos os serviços", "Instalação", "Troca", "Retirada", "Manutenção"}); filters.addView(service, mb(8));
        Spinner tech = spinner(technicianFilters(entries)); filters.addView(tech); list.addView(filters, mb(11));
        TextView count = txt("", 12, themeMuted(), true); list.addView(count, mb(8));
        LinearLayout holder = new LinearLayout(this); holder.setOrientation(LinearLayout.VERTICAL); list.addView(holder);

        Runnable renderHistory = () -> {
            holder.removeAllViews(); int n = 0; String query = q.getText().toString().trim().toLowerCase(Locale.ROOT); String sf = service.getSelectedItem().toString(); String tf = tech.getSelectedItem().toString();
            for (ServiceStore.Entry e : entries) {
                if (!"Todos os serviços".equals(sf) && !sf.equals(e.service)) continue;
                if (!"Todos os técnicos".equals(tf) && !tf.equals(e.technician)) continue;
                String hay = (e.plate + " " + e.module + " " + e.technician + " " + e.service).toLowerCase(Locale.ROOT); if (!query.isEmpty() && !hay.contains(query)) continue;
                n++; LinearLayout c = serviceEntryCard(e); holder.addView(c, mb(9));
            }
            count.setText(n + " serviços encontrados");
            if (n == 0) { TextView empty = txt("Nenhum serviço corresponde aos filtros.", 13, themeMuted(), false); holder.addView(empty); }
        };
        q.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){renderHistory.run();} public void afterTextChanged(Editable e){} });
        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){renderHistory.run();}};
        service.setOnItemSelectedListener(l); tech.setOnItemSelectedListener(l); renderHistory.run();
    }

    LinearLayout serviceEntryCard(ServiceStore.Entry e) {
        LinearLayout c = card(); LinearLayout top = row();
        TextView plate = txt(e.plate, 18, themeText(), true); top.addView(plate, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = txt(e.service.toUpperCase(Locale.ROOT), 9, RED, true); badge.setGravity(Gravity.CENTER); badge.setBackground(round(themeSurface2(), 99, themeStroke(), 1)); badge.setPadding(dp(10), 0, dp(10), 0); top.addView(badge, new LinearLayout.LayoutParams(-2, dp(30))); c.addView(top, mb(5));
        c.addView(txt(e.finishedAt + "  •  " + (e.technician.isEmpty() ? "Técnico não informado" : e.technician), 12, themeMuted(), false), mb(4));
        String mod = (e.brand == null || e.brand.isEmpty() || "Não informar".equals(e.brand)) ? "" : e.brand; if (e.module != null && !e.module.isEmpty()) mod += (mod.isEmpty()?"":" • ") + e.module;
        if (!mod.isEmpty()) c.addView(txt(mod, 12, themeText(), false), mb(8));
        c.addView(button("DETALHES", false, v -> showServiceDetail(e)), new LinearLayout.LayoutParams(-1, dp(42))); return c;
    }

    void showServiceDetail(ServiceStore.Entry e) {
        StringBuilder s = new StringBuilder(); s.append("Placa: ").append(e.plate).append("\nServiço: ").append(e.service).append("\nTécnico: ").append(e.technician.isEmpty()?"-":e.technician).append("\nData: ").append(e.finishedAt);
        if (e.brand != null && !e.brand.isEmpty() && !"Não informar".equals(e.brand)) s.append("\nMódulo: ").append(e.brand);
        if (e.module != null && !e.module.isEmpty()) s.append("\nNúmero: ").append(e.module);
        new AlertDialog.Builder(this).setTitle("Serviço • " + e.plate).setMessage(s.toString()).setNegativeButton("Fechar", null).setPositiveButton("REMOVER", (d,w) -> new AlertDialog.Builder(this).setTitle("Remover registro?").setMessage("Isso remove somente o item do histórico local.").setNegativeButton("Cancelar", null).setPositiveButton("REMOVER", (a,b) -> { ServiceStore.remove(this, e); serviceHistoryScreen(); }).show()).show();
    }

    String[] technicianFilters(ArrayList<ServiceStore.Entry> entries) {
        LinkedHashSet<String> names = new LinkedHashSet<>(); names.add("Todos os técnicos"); names.add("Eduardo"); names.add("Rafael");
        for (ServiceStore.Entry e : entries) if (e.technician != null && !e.technician.trim().isEmpty()) names.add(e.technician);
        return names.toArray(new String[0]);
    }

    void dashboardScreen() {
        screenHeader("DASHBOARD", "Visão mensal do trabalho salvo neste aparelho.", "DASHBOARD");
        ArrayList<ServiceStore.Entry> entries = ServiceStore.list(this);
        LinearLayout f = card(); f.addView(label("MÊS")); Spinner month = spinner(monthOptions(entries)); f.addView(month, mb(8));
        f.addView(label("TIPO DE SERVIÇO")); Spinner service = spinner(new String[]{"Todos", "Instalação", "Troca", "Retirada", "Manutenção"}); f.addView(service, mb(8));
        f.addView(label("TÉCNICO")); Spinner tech = spinner(dashboardTechFilters(entries)); f.addView(tech); list.addView(f, mb(11));
        LinearLayout holder = new LinearLayout(this); holder.setOrientation(LinearLayout.VERTICAL); list.addView(holder);
        Runnable renderDash = () -> renderDashboard(holder, entries, month.getSelectedItem().toString(), service.getSelectedItem().toString(), tech.getSelectedItem().toString());
        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){renderDash.run();}};
        month.setOnItemSelectedListener(l); service.setOnItemSelectedListener(l); tech.setOnItemSelectedListener(l); renderDash.run();
    }

    String[] monthOptions(ArrayList<ServiceStore.Entry> entries) {
        LinkedHashSet<String> set = new LinkedHashSet<>(); set.add(new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(new Date()));
        ArrayList<String> others = new ArrayList<>(); SimpleDateFormat f = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        for (ServiceStore.Entry e : entries) if (e.epoch > 0) { String m = f.format(new Date(e.epoch)); if (!set.contains(m) && !others.contains(m)) others.add(m); }
        Collections.sort(others, Collections.reverseOrder()); set.addAll(others); return set.toArray(new String[0]);
    }

    String[] dashboardTechFilters(ArrayList<ServiceStore.Entry> entries) {
        LinkedHashSet<String> n = new LinkedHashSet<>(); n.add("Todos"); n.add("Eduardo"); n.add("Rafael"); for (ServiceStore.Entry e : entries) if (e.technician != null && !e.technician.isEmpty()) n.add(e.technician); return n.toArray(new String[0]);
    }

    void renderDashboard(LinearLayout holder, ArrayList<ServiceStore.Entry> allEntries, String month, String service, String tech) {
        holder.removeAllViews(); ArrayList<ServiceStore.Entry> entries = new ArrayList<>();
        SimpleDateFormat mf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        for (ServiceStore.Entry e : allEntries) {
            String em = e.epoch > 0 ? mf.format(new Date(e.epoch)) : ""; if (!month.equals(em)) continue;
            if (!"Todos".equals(service) && !service.equals(e.service)) continue; if (!"Todos".equals(tech) && !tech.equals(e.technician)) continue; entries.add(e);
        }
        int installs = 0; HashSet<String> plates = new HashSet<>(); for (ServiceStore.Entry e : entries) { if ("Instalação".equals(e.service)) installs++; plates.add(e.plate); }
        LinearLayout stats = row(); stats.addView(metric("SERVIÇOS", String.valueOf(entries.size())), new LinearLayout.LayoutParams(0, dp(86), 1)); gap(stats); stats.addView(metric("INSTALAÇÕES", String.valueOf(installs)), new LinearLayout.LayoutParams(0, dp(86), 1)); gap(stats); stats.addView(metric("PLACAS", String.valueOf(plates.size())), new LinearLayout.LayoutParams(0, dp(86), 1)); holder.addView(stats, mb(12));
        LinearLayout cal = card(); cal.addView(txt("CALENDÁRIO • " + month, 14, themeText(), true), mb(10)); buildCalendar(cal, entries, month); holder.addView(cal, mb(12)); animateIn(cal, 60);
        if (entries.isEmpty()) { LinearLayout e = card(); e.addView(txt("Sem serviços neste período.", 14, themeText(), true), mb(3)); e.addView(txt("Os atendimentos aparecem aqui automaticamente quando os relatórios são gerados.", 12, themeMuted(), false)); holder.addView(e); }
    }

    LinearLayout metric(String label, String value) {
        LinearLayout m = new LinearLayout(this); m.setOrientation(LinearLayout.VERTICAL); m.setGravity(Gravity.CENTER); m.setBackground(round(themeSurface(), 16, themeStroke(), 1));
        TextView v = txt(value, 23, themeText(), true); v.setGravity(Gravity.CENTER); m.addView(v); TextView l = txt(label, 9, themeMuted(), true); l.setGravity(Gravity.CENTER); m.addView(l); return m;
    }

    void buildCalendar(LinearLayout host, ArrayList<ServiceStore.Entry> entries, String month) {
        HashMap<Integer,Integer> counts = new HashMap<>();
        try {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            for (ServiceStore.Entry e : entries) {
                if (e.epoch <= 0) continue; Calendar c = Calendar.getInstance(); c.setTimeInMillis(e.epoch); int d = c.get(Calendar.DAY_OF_MONTH); counts.put(d, counts.containsKey(d) ? counts.get(d) + 1 : 1);
            }
            String[] parts = month.split("/"); int mo = Integer.parseInt(parts[0]) - 1, year = Integer.parseInt(parts[1]); Calendar first = Calendar.getInstance(); first.set(year, mo, 1, 12, 0, 0); int offset = first.get(Calendar.DAY_OF_WEEK) - 1; int max = first.getActualMaximum(Calendar.DAY_OF_MONTH);
            LinearLayout head = row(); String[] names = {"D","S","T","Q","Q","S","S"}; for (String n : names) { TextView x = txt(n, 10, themeMuted(), true); x.setGravity(Gravity.CENTER); head.addView(x, new LinearLayout.LayoutParams(0, dp(28), 1)); } host.addView(head);
            int day = 1;
            for (int week = 0; week < 6; week++) {
                LinearLayout row = row();
                for (int col = 0; col < 7; col++) {
                    int cell = week * 7 + col; LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setBackground(round(themeSurface2(), 10, themeStroke(), 1));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1); lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                    if (cell >= offset && day <= max) {
                        final int d = day; TextView dn = txt(String.valueOf(day), 12, themeText(), true); dn.setGravity(Gravity.CENTER); box.addView(dn);
                        int ct = counts.containsKey(day) ? counts.get(day) : 0; TextView num = txt(ct == 0 ? "·" : ct + " serv.", 9, ct == 0 ? themeMuted() : RED, ct > 0); num.setGravity(Gravity.CENTER); box.addView(num);
                        if (ct > 0) box.setOnClickListener(v -> showDayEntries(entries, year, mo, d)); day++;
                    }
                    row.addView(box, lp);
                }
                host.addView(row);
                if (day > max) break;
            }
        } catch (Exception e) { host.addView(txt("Não foi possível montar o calendário.", 12, themeMuted(), false)); }
    }

    void showDayEntries(ArrayList<ServiceStore.Entry> entries, int year, int month, int day) {
        StringBuilder b = new StringBuilder(); int n = 0;
        for (ServiceStore.Entry e : entries) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(e.epoch); if (c.get(Calendar.YEAR)==year && c.get(Calendar.MONTH)==month && c.get(Calendar.DAY_OF_MONTH)==day) { n++; b.append(e.plate).append(" • ").append(e.service).append(" • ").append(e.technician).append("\n"); } }
        new AlertDialog.Builder(this).setTitle(day + "/" + (month+1) + " • " + n + " serviços").setMessage(b.toString()).setPositiveButton("OK", null).show();
    }

    @Override void toolsScreen() {
        screenHeader("FERRAMENTAS", "Portais e utilitários usados no atendimento.", "FERRAMENTAS");
        tool("SETUP COMCEPT", "https://setup.concept.inf.br/login");
        tool("PLATAFORMA TAKI", "https://app.takirastreadores.com.br/app");
        tool("VIRTUEYES", "https://veye.com.br/veye/dispositivos");
        list.addView(button("WAZE • GPS", false, v -> openPreferred("https://waze.com/ul", "com.waze")), mb(8));
        list.addView(button("GOOGLE MAPS", false, v -> openPreferred("https://www.google.com/maps", "com.google.android.apps.maps")), mb(8));
        tool("SPEEDTEST", "https://www.speedtest.net");
        list.addView(button("ABRIR CENTRAL DE GPS TAKI", true, v -> gpsScreen()), mb(8));
        tool("REPOSITÓRIO / VERSÕES", "https://github.com/SrRafa13/TAKI_FIELD_ANDROID");
        list.addView(button("VERIFICAR ATUALIZAÇÃO", false, v -> checkUpdate()), mt(10));
    }

    @Override void settingsScreen() {
        screenHeader("AJUSTES", "Personalização, técnico e configurações locais.", "AJUSTES");
        LinearLayout theme = card(); theme.addView(txt("APARÊNCIA", 15, themeText(), true), mb(4)); theme.addView(txt("Escolha entre tema preto e tema branco.", 12, themeMuted(), false), mb(10));
        LinearLayout tr = row(); Button dark = button("TEMA PRETO", !lightMode(), v -> setThemeMode(false)); Button light = button("TEMA BRANCO", lightMode(), v -> setThemeMode(true)); tr.addView(dark, new LinearLayout.LayoutParams(0, dp(48), 1)); gap(tr); tr.addView(light, new LinearLayout.LayoutParams(0, dp(48), 1)); theme.addView(tr); list.addView(theme, mb(11));

        LinearLayout technician = card(); technician.addView(txt("TÉCNICO ATIVO", 15, themeText(), true), mb(4)); technician.addView(txt(prefs.getString("technician", "-"), 20, RED, true), mb(9));
        TextView tiny = txt("trocar de técnico", 11, themeMuted(), false); tiny.setOnClickListener(v -> { prefs.edit().remove("technician").apply(); welcomeScreen(); }); technician.addView(tiny); list.addView(technician, mb(11));

        LinearLayout company = card(); company.addView(txt("CONFIGURAÇÕES DA EMPRESA", 15, themeText(), true), mb(4)); company.addView(txt("Salvas somente neste aparelho. Não entram no código público.", 12, themeMuted(), false), mb(10));
        EditText op=input("Operadora / perfil");op.setText(prefs.getString("cfg_operator",""));company.addView(op,mb(8));
        EditText apn=input("APN");apn.setText(prefs.getString("cfg_apn",""));company.addView(apn,mb(8));
        EditText usr=input("Usuário APN");usr.setText(prefs.getString("cfg_user",""));company.addView(usr,mb(8));
        EditText pass=input("Senha APN");pass.setText(prefs.getString("cfg_pass",""));company.addView(pass,mb(8));
        EditText srv=input("Servidor / IP");srv.setText(prefs.getString("cfg_server",""));company.addView(srv,mb(8));
        EditText port=input("Porta");port.setText(prefs.getString("cfg_port",""));company.addView(port,mb(8));
        EditText qpass=input("Senha Queclink padrão");qpass.setText(prefs.getString("cfg_qlpass",""));company.addView(qpass,mb(10));
        company.addView(button("SALVAR CONFIGURAÇÕES",true,v->{prefs.edit().putString("cfg_operator",op.getText().toString().trim()).putString("cfg_apn",apn.getText().toString().trim()).putString("cfg_user",usr.getText().toString().trim()).putString("cfg_pass",pass.getText().toString()).putString("cfg_server",srv.getText().toString().trim()).putString("cfg_port",port.getText().toString().trim()).putString("cfg_qlpass",qpass.getText().toString()).apply();Toast.makeText(this,"Configurações salvas localmente",Toast.LENGTH_SHORT).show();}),new LinearLayout.LayoutParams(-1,dp(50))); list.addView(company, mb(11));

        LinearLayout data = card(); ArrayList<ServiceStore.Entry> history = ServiceStore.list(this); data.addView(txt("DADOS LOCAIS", 15, themeText(), true), mb(4)); data.addView(txt(history.size()+" serviços no histórico • "+loadDestinations().size()+" destinos GPS salvos",12,themeMuted(),false),mb(9));
        data.addView(button("LIMPAR HISTÓRICO DE SERVIÇOS", false, v -> new AlertDialog.Builder(this).setTitle("Limpar histórico?").setMessage("Os PDFs em Downloads não serão apagados. Somente o índice local do TAKI FIELD será removido.").setNegativeButton("Cancelar",null).setPositiveButton("LIMPAR",(d,w)->{ServiceStore.clear(this);Toast.makeText(this,"Histórico local limpo",Toast.LENGTH_SHORT).show();settingsScreen();}).show()),new LinearLayout.LayoutParams(-1,dp(46))); list.addView(data, mb(11));
        list.addView(button("VERIFICAR ATUALIZAÇÃO", true, v -> checkUpdate()), new LinearLayout.LayoutParams(-1,dp(50)));
    }

    void setThemeMode(boolean light) { prefs.edit().putString("theme_mode", light ? "light" : "dark").apply(); recreate(); }

    @Override void checkUpdate() {
        Toast.makeText(this,"Consultando versão...",Toast.LENGTH_SHORT).show();
        new Thread(()->{try{URL u=new URL("https://raw.githubusercontent.com/SrRafa13/TAKI_FIELD_ANDROID/main/version.txt");BufferedReader br=new BufferedReader(new InputStreamReader(u.openStream()));String latest=br.readLine();br.close();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("TAKI FIELD • Atualização").setMessage("Versão instalada: "+VERSION_60+"\nVersão disponível: "+latest+(VERSION_60.equals(latest)?"\n\nSeu app está atualizado.":"\n\nHá uma versão diferente disponível.")).setNegativeButton("Fechar",null).setPositiveButton("ABRIR VERSÕES",(d,w)->openUrl("https://github.com/SrRafa13/TAKI_FIELD_ANDROID/actions")).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Não foi possível consultar atualização",Toast.LENGTH_LONG).show());}}).start();
    }

    @Override void backHeader(String title, String sub) { screenHeader(title, sub, ""); }

    void screenHeader(String title, String sub, String active) {
        currentSection = active == null ? "" : active; list.removeAllViews(); applyBackground();
        LinearLayout top = row();
        LinearLayout brand = new LinearLayout(this); brand.setOrientation(LinearLayout.VERTICAL); brand.addView(txt("TAKI FIELD", 21, themeText(), true)); brand.addView(txt("FIELD OPS • 6.0", 9, RED, true)); top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout who = new LinearLayout(this); who.setOrientation(LinearLayout.VERTICAL); who.setGravity(Gravity.END); TextView tech = txt("●  " + prefs.getString("technician", "-"), 11, themeText(), true); tech.setGravity(Gravity.END); who.addView(tech); TextView swap = txt("trocar técnico", 10, themeMuted(), false); swap.setGravity(Gravity.END); swap.setOnClickListener(v -> { prefs.edit().remove("technician").apply(); welcomeScreen(); }); who.addView(swap); top.addView(who); list.addView(top, mb(12));
        list.addView(navBar(active), mb(17));
        list.addView(txt(title, 24, themeText(), true), mb(4)); if (sub != null && !sub.isEmpty()) list.addView(txt(sub, 13, themeMuted(), false), mb(14));
    }

    HorizontalScrollView navBar(String active) {
        HorizontalScrollView s = new HorizontalScrollView(this); s.setHorizontalScrollBarEnabled(false); s.setFillViewport(false);
        LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setPadding(0, dp(2), dp(8), dp(2));
        r.addView(navChip("◎ CENTRAL", "CENTRAL".equals(active), v -> centralScreen()));
        r.addView(navChip("⌘ COMANDOS", "COMANDOS".equals(active), v -> commandsScreen()));
        r.addView(navChip("⌖ GPS", "GPS".equals(active), v -> gpsScreen()));
        r.addView(navChip("▦ HISTÓRICO", "HISTÓRICO".equals(active), v -> serviceHistoryScreen()));
        r.addView(navChip("◫ DASHBOARD", "DASHBOARD".equals(active), v -> dashboardScreen()));
        r.addView(navChip("◇ FERRAMENTAS", "FERRAMENTAS".equals(active), v -> toolsScreen()));
        r.addView(navChip("⚙ AJUSTES", "AJUSTES".equals(active), v -> settingsScreen())); s.addView(r); return s;
    }

    TextView navChip(String text, boolean selected, View.OnClickListener l) {
        TextView v = txt(text, 11, selected ? Color.WHITE : themeText(), true); v.setGravity(Gravity.CENTER); v.setPadding(dp(14), 0, dp(14), 0); v.setBackground(round(selected ? RED : themeSurface2(), 99, selected ? RED : themeStroke(), 1)); v.setOnClickListener(l); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(40)); p.rightMargin = dp(7); v.setLayoutParams(p); return v;
    }

    void animateIn(View v, long delay) { v.setAlpha(0f); v.setTranslationY(dp(8)); v.animate().alpha(1f).translationY(0).setStartDelay(delay).setDuration(220).start(); }
    void applyBackground() { if (root != null) root.setBackgroundColor(themeBg()); if (list != null) list.setBackgroundColor(themeBg()); }
    boolean lightMode() { return "light".equals(prefs == null ? "dark" : prefs.getString("theme_mode", "dark")); }
    int themeBg() { return lightMode() ? Color.rgb(244,245,247) : Color.rgb(7,8,10); }
    int themeSurface() { return lightMode() ? Color.rgb(255,255,255) : Color.rgb(18,19,23); }
    int themeSurface2() { return lightMode() ? Color.rgb(236,238,242) : Color.rgb(28,29,34); }
    int themeText() { return lightMode() ? Color.rgb(25,27,31) : Color.rgb(246,247,249); }
    int themeMuted() { return lightMode() ? Color.rgb(103,107,116) : Color.rgb(155,158,166); }
    int themeStroke() { return lightMode() ? Color.rgb(214,217,223) : Color.rgb(48,50,58); }

    @Override TextView txt(String s, int size, int color, boolean bold) {
        int mapped = color; if (color == WHITE) mapped = themeText(); else if (color == MUTED) mapped = themeMuted();
        TextView v = new TextView(this); v.setText(s); v.setTextColor(mapped); v.setTextSize(size); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v;
    }

    @Override LinearLayout card() {
        LinearLayout x = new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, lightMode() ? new int[]{Color.WHITE,Color.rgb(248,249,250)} : new int[]{Color.rgb(22,23,27),Color.rgb(13,14,17)}); g.setCornerRadius(dp(18)); g.setStroke(dp(1), themeStroke()); x.setBackground(g); x.setElevation(dp(2)); return x;
    }

    @Override EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setHintTextColor(themeMuted()); e.setTextColor(themeText()); e.setTextSize(15); e.setSingleLine(true); e.setBackground(round(themeSurface2(),13,themeStroke(),1)); e.setPadding(dp(14),0,dp(14),0); e.setMinHeight(dp(52)); return e;
    }

    @Override Button button(String s, boolean red, View.OnClickListener l) {
        Button b = new Button(this); b.setText(s); b.setTextColor(red ? Color.WHITE : themeText()); b.setTextSize(12); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setAllCaps(false); b.setStateListAnimator(null);
        GradientDrawable g = red ? new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{RED,Color.rgb(198,24,38)}) : new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, lightMode()?new int[]{Color.rgb(242,243,246),Color.rgb(232,234,239)}:new int[]{Color.rgb(39,40,46),Color.rgb(28,29,34)}); g.setCornerRadius(dp(14)); g.setStroke(dp(1), red?Color.rgb(255,74,86):themeStroke()); b.setBackground(g); if(l!=null)b.setOnClickListener(l); return b;
    }

    @Override Spinner spinner(String[] a) { Spinner s = new Spinner(this); s.setAdapter(adapter(a)); s.setBackground(round(themeSurface2(),13,themeStroke(),1)); s.setPadding(dp(10),0,dp(10),0); s.setMinimumHeight(dp(52)); return s; }

    @Override android.widget.ArrayAdapter<String> adapter(String[] a) {
        return new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, a) {
            @Override public View getView(int p, View c, ViewGroup g) { TextView v=(TextView)super.getView(p,c,g);v.setTextColor(themeText());v.setTextSize(15);return v; }
            @Override public View getDropDownView(int p, View c, ViewGroup g) { TextView v=(TextView)super.getDropDownView(p,c,g);v.setTextColor(themeText());v.setBackgroundColor(themeSurface());v.setPadding(dp(16),dp(14),dp(16),dp(14));return v; }
        };
    }
}

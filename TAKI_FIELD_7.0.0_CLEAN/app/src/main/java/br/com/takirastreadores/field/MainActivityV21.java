package br.com.takirastreadores.field;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

public class MainActivityV21 extends MainActivity {

    static final String RS232_VENDOR = "RS232 / GetScale";

    @Override
    void seed() {
        super.seed();
        String[] R = {"GetScale / RS232"};
        add(RS232_VENDOR,
                "Solicita identificação nas macros",
                "Comando GetScale/RS232 para solicitar identificação nas macros.",
                "833|1|",
                R);
        add(RS232_VENDOR,
                "Voltar protocolo padrão (remoto)",
                "Retorna remotamente ao protocolo padrão.",
                "711|0|",
                R);
        add(RS232_VENDOR,
                "Passar protocolo para Queclink",
                "Altera o protocolo para Queclink.",
                "817|8|",
                R);
    }

    @Override
    void home() {
        super.home();

        idField.setHint("Número do módulo / ESN / IMEI");
        idField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            public void afterTextChanged(Editable s) {}
        });

        vendor.setAdapter(adapter(new String[]{"Suntech", "X3Tech", "Queclink", RS232_VENDOR}));

        LinearLayout links = row();
        links.addView(button("SETUP", false, v -> openUrl("https://setup.concept.inf.br/login")),
                new LinearLayout.LayoutParams(0, dp(48), 1));
        Space gap = new Space(this);
        links.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        links.addView(button("PLATAFORMA TAKI", true, v -> openUrl("https://app.takirastreadores.com.br/app")),
                new LinearLayout.LayoutParams(0, dp(48), 1));

        // Header, seletor, busca e atalhos já ocupam as quatro primeiras posições.
        list.addView(links, 4, mb(12));
    }

    @Override
    String[] models(String v) {
        if (RS232_VENDOR.equals(v)) return new String[]{"GetScale / RS232"};
        return super.models(v);
    }

    @Override
    String resolve(String s) {
        String id = idField == null ? "" : idField.getText().toString().trim();
        if (id.isEmpty()) id = "XXX";
        return s.replace("XXXX", id).replace("XXX", id);
    }

    @Override
    View cmdCard(Cmd c) {
        LinearLayout box = card();
        TextView title = txt(c.title, 16, WHITE, true);
        box.addView(title);
        box.addView(txt(c.desc, 12, MUTED, false), mb(7));

        TextView preview = txt(resolve(c.template), 13, Color.rgb(255, 190, 80), false);
        preview.setTypeface(Typeface.MONOSPACE);
        preview.setTextIsSelectable(true);
        box.addView(preview, mb(10));

        if (RS232_VENDOR.equals(c.vendor)) {
            box.addView(button("COPIAR COMANDO RS232", true, v -> copy(resolve(c.template))),
                    new LinearLayout.LayoutParams(-1, dp(46)));
            return box;
        }

        LinearLayout r = row();
        r.addView(button(c.fields.length > 0 ? "GERAR" : "COPIAR", true, v -> {
            if (c.fields.length > 0) generate(c); else copy(resolve(c.template));
        }), new LinearLayout.LayoutParams(0, dp(46), 1));
        Space sp = new Space(this);
        r.addView(sp, new LinearLayout.LayoutParams(dp(8), 1));
        r.addView(button("SMS", false, v -> {
            if (c.fields.length > 0) generateForSms(c); else sms(resolve(c.template));
        }), new LinearLayout.LayoutParams(0, dp(46), 1));
        box.addView(r);
        return box;
    }

    void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Não foi possível abrir o endereço", android.widget.Toast.LENGTH_LONG).show();
        }
    }
}

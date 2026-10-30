package br.com.takirastreadores.field;

import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class InstallationActivityV60 extends InstallationActivity {
    android.content.SharedPreferences appPrefs;

    @Override public void onCreate(Bundle b) {
        appPrefs = getSharedPreferences("taki", MODE_PRIVATE);
        super.onCreate(b);
        if (list != null) list.setBackgroundColor(themeBg());
        replaceVersionText(list);
        try {
            getWindow().setStatusBarColor(themeBg());
            getWindow().setNavigationBarColor(themeBg());
            if (lightMode()) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } catch (Exception ignored) {}
    }

    void replaceVersionText(View v) {
        if (v instanceof TextView) {
            TextView t = (TextView)v;
            if ("TAKI FIELD • V5".contentEquals(t.getText())) t.setText("TAKI FIELD • V6");
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); i++) replaceVersionText(g.getChildAt(i));
        }
    }

    String currentTech() {
        if (appPrefs == null) appPrefs = getSharedPreferences("taki", MODE_PRIVATE);
        String t = appPrefs.getString("technician", "").trim();
        return t.isEmpty() ? "Não informado" : t;
    }

    @Override void generateSinglePdf(ServiceRecord r) {
        ServiceStore.add(this, r.plate, r.service, r.brand, r.module, currentTech(), r.finishedAt, System.currentTimeMillis(), "");
        super.generateSinglePdf(r);
    }

    @Override void generateBatchPdf() {
        if (!batchRecords.isEmpty()) {
            String batchId = "LOTE_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            long now = System.currentTimeMillis();
            for (ServiceRecord r : new ArrayList<>(batchRecords)) {
                ServiceStore.add(this, r.plate, r.service, r.brand, r.module, currentTech(), r.finishedAt, now, batchId);
            }
        }
        super.generateBatchPdf();
    }

    @Override int drawHeader(Canvas c, String p, int y, String subtitle) {
        int out = super.drawHeader(c, p, y, subtitle);
        Paint tech = new Paint(Paint.ANTI_ALIAS_FLAG);
        tech.setColor(Color.DKGRAY);
        tech.setTextSize(10);
        tech.setTypeface(Typeface.DEFAULT_BOLD);
        tech.setTextAlign(Paint.Align.RIGHT);
        c.drawText("TÉC. " + currentTech().toUpperCase(Locale.ROOT), 547, y + 24, tech);
        return out;
    }

    @Override int writeBatchCover(PdfDocument doc, int pageNo) {
        PdfDocument.Page page = newPage(doc, pageNo++);
        Canvas c = page.getCanvas();
        drawTopBand(c);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(25,25,25));
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(28);
        c.drawText("TAKI FIELD",48,72,p);
        p.setTextSize(17);
        p.setColor(RED);
        c.drawText("RELATÓRIO EM LOTE",48,105,p);
        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.DKGRAY);
        body.setTextSize(13);
        body.setTypeface(Typeface.DEFAULT_BOLD);
        c.drawText("Responsável técnico: " + currentTech(),48,136,body);
        body.setTypeface(Typeface.DEFAULT);
        c.drawText(batchRecords.size()+" veículos • gerado em "+new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date()),48,158,body);
        int y=198;
        Paint item=new Paint(Paint.ANTI_ALIAS_FLAG);item.setTextSize(14);item.setColor(Color.rgb(40,40,40));
        for(int i=0;i<batchRecords.size();i++){
            if(y>790){doc.finishPage(page);page=newPage(doc,pageNo++);c=page.getCanvas();drawTopBand(c);y=70;}
            ServiceRecord r=batchRecords.get(i);
            item.setTypeface(Typeface.DEFAULT_BOLD);c.drawText(String.format(Locale.ROOT,"%02d",i+1)+"   "+r.plate,48,y,item);
            item.setTypeface(Typeface.DEFAULT);c.drawText(r.service+(r.module.isEmpty()?"":" • "+r.module),210,y,item);y+=27;
        }
        doc.finishPage(page);return pageNo;
    }

    boolean lightMode() {
        if (appPrefs == null) appPrefs = getSharedPreferences("taki", MODE_PRIVATE);
        return "light".equals(appPrefs.getString("theme_mode", "dark"));
    }

    int themeBg(){return lightMode()?Color.rgb(244,245,247):Color.rgb(7,8,10);}
    int themeSurface(){return lightMode()?Color.rgb(255,255,255):Color.rgb(18,19,23);}
    int themeSurface2(){return lightMode()?Color.rgb(236,238,242):Color.rgb(28,29,34);}
    int themeText(){return lightMode()?Color.rgb(25,27,31):Color.rgb(246,247,249);}
    int themeMuted(){return lightMode()?Color.rgb(103,107,116):Color.rgb(155,158,166);}
    int themeStroke(){return lightMode()?Color.rgb(214,217,223):Color.rgb(48,50,58);}

    @Override TextView txt(String s,int size,int color,boolean bold){
        int mapped=color;if(color==WHITE)mapped=themeText();else if(color==MUTED)mapped=themeMuted();
        TextView v=new TextView(this);v.setText(s);v.setTextColor(mapped);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }

    @Override LinearLayout card(){
        LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,lightMode()?new int[]{Color.WHITE,Color.rgb(248,249,250)}:new int[]{Color.rgb(21,22,26),Color.rgb(13,14,17)});g.setCornerRadius(dp(18));g.setStroke(dp(1),themeStroke());x.setBackground(g);x.setElevation(dp(2));return x;
    }

    @Override EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(themeMuted());e.setTextColor(themeText());e.setTextSize(15);e.setSingleLine(true);e.setBackground(round(themeSurface2(),13,themeStroke(),1));e.setPadding(dp(14),0,dp(14),0);e.setMinHeight(dp(52));return e;}

    @Override Spinner spinner(String[] a){
        Spinner s=new Spinner(this);
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,a){
            @Override public View getView(int p,View c,ViewGroup g){TextView v=(TextView)super.getView(p,c,g);v.setTextColor(themeText());v.setTextSize(15);v.setPadding(dp(10),0,dp(10),0);return v;}
            @Override public View getDropDownView(int p,View c,ViewGroup g){TextView v=(TextView)super.getDropDownView(p,c,g);v.setTextColor(themeText());v.setBackgroundColor(themeSurface());v.setPadding(dp(16),dp(14),dp(16),dp(14));return v;}
        };
        s.setAdapter(ad);s.setBackground(round(themeSurface2(),13,themeStroke(),1));s.setMinimumHeight(dp(52));return s;
    }

    @Override Button button(String s,boolean red,View.OnClickListener l){
        Button b=new Button(this);b.setText(s);b.setTextColor(red?Color.WHITE:themeText());b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setBackground(round(red?RED:themeSurface2(),14,red?RED2:themeStroke(),1));b.setOnClickListener(l);b.setStateListAnimator(null);return b;
    }

    @Override Button segment(String s,boolean selected,View.OnClickListener l){
        Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setTextColor(selected?Color.WHITE:themeMuted());b.setBackground(segmentBg(selected));b.setOnClickListener(l);b.setStateListAnimator(null);return b;
    }

    @Override GradientDrawable segmentBg(boolean selected){return round(selected?RED:themeSurface2(),14,selected?RED2:themeStroke(),1);}

    @Override GradientDrawable round(int color,int radius,int strokeColor,int stroke){
        int c=color,sc=strokeColor;
        if(lightMode()){
            if(color==PANEL2||color==PANEL||color==BG)c=themeSurface2();
            if(strokeColor==Color.rgb(48,50,57)||strokeColor==Color.rgb(52,54,61)||strokeColor==Color.rgb(54,56,64)||strokeColor==Color.rgb(37,39,45))sc=themeStroke();
        }
        GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(stroke),sc);return g;
    }
}

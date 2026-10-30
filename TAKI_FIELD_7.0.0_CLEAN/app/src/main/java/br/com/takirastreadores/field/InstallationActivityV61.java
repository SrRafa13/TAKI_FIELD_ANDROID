package br.com.takirastreadores.field;

import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class InstallationActivityV61 extends InstallationActivityV60 {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        replaceV61(list);
        if (list != null) list.setBackgroundColor(themeBg());
        try {
            getWindow().setStatusBarColor(themeBg());
            getWindow().setNavigationBarColor(themeBg());
            getWindow().getDecorView().setSystemUiVisibility(lightMode() ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0);
        } catch (Exception ignored) {}
    }

    void replaceV61(View v) {
        if (v instanceof TextView) {
            TextView t=(TextView)v;
            if ("TAKI FIELD • V6".contentEquals(t.getText())) t.setText("TAKI FIELD • V6.1");
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)replaceV61(g.getChildAt(i));}
    }

    @Override void generateSinglePdf(ServiceRecord r) {
        try {
            PdfDocument doc=new PdfDocument();
            writeRecord(doc,r,1,false);
            String filename="TAKI_"+safe(r.service)+"_"+r.plate+"_"+stamp()+".pdf";
            Uri out=savePdf(doc,filename);
            doc.close();
            String archive=archiveFromUri(out,filename);
            ServiceStore.add(this,r.plate,r.service,r.brand,r.module,currentTech(),r.finishedAt,System.currentTimeMillis(),"",archive,"individual",1);
            showDone(filename,out,false);
        } catch(Exception e){Toast.makeText(this,"Erro ao gerar PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    @Override void generateBatchPdf() {
        if(batchRecords.isEmpty()){Toast.makeText(this,"Finalize pelo menos uma placa para gerar o lote",Toast.LENGTH_LONG).show();return;}
        ArrayList<ServiceRecord> done=new ArrayList<>(batchRecords);
        try {
            PdfDocument doc=new PdfDocument();
            int page=1;
            page=writeBatchCover(doc,page);
            for(ServiceRecord r:done)page=writeRecord(doc,r,page,true);
            String filename="TAKI_LOTE_"+done.size()+"_PLACAS_"+stamp()+".pdf";
            Uri out=savePdf(doc,filename);
            doc.close();
            String archive=archiveFromUri(out,filename);
            String batchId="LOTE_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());
            long now=System.currentTimeMillis();
            for(ServiceRecord r:done){
                ServiceStore.add(this,r.plate,r.service,r.brand,r.module,currentTech(),r.finishedAt,now,batchId,archive,"batch",done.size());
            }

            // Finalização definitiva: impede onPause/onStop de recriar um rascunho vazio.
            saveHandler.removeCallbacks(saveRunnable);
            restoring=true;
            batchMode=false;
            batchRecords.clear();
            clearCurrent();
            if(draftFile().exists())draftFile().delete();
            restoring=false;
            setMode(false);
            updateBatchStatus();
            showDone(filename,out,true);
        } catch(Exception e){
            restoring=false;
            Toast.makeText(this,"Erro ao gerar PDF do lote: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    String archiveFromUri(Uri uri,String filename) {
        File dir=new File(getFilesDir(),"reports");dir.mkdirs();
        File out=new File(dir,filename);
        try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream fos=new FileOutputStream(out)){
            if(in==null)throw new IOException("PDF indisponível");
            byte[] b=new byte[8192];int n;while((n=in.read(b))>0)fos.write(b,0,n);
            return filename;
        }catch(Exception e){return "uri:"+uri.toString();}
    }

    ThemeEngine.Theme theme(){if(appPrefs==null)appPrefs=getSharedPreferences("taki",MODE_PRIVATE);return ThemeEngine.current(appPrefs);}
    @Override boolean lightMode(){return theme().light;}
    @Override int themeBg(){return theme().bg;}
    @Override int themeSurface(){return theme().surface;}
    @Override int themeSurface2(){return theme().surface2;}
    @Override int themeText(){return theme().text;}
    @Override int themeMuted(){return theme().muted;}
    @Override int themeStroke(){return theme().stroke;}

    @Override LinearLayout card(){
        LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{theme().surface,theme().surface2});
        g.setCornerRadius(dp(18));g.setStroke(dp(1),theme().stroke);x.setBackground(g);x.setElevation(dp(2));return x;
    }

    @Override Button button(String s,boolean red,View.OnClickListener l){
        Button b=new Button(this);b.setText(s);b.setTextColor(red?Color.WHITE:theme().text);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);
        b.setBackground(round(red?theme().accent:theme().surface2,14,red?theme().accent2:theme().stroke,1));b.setOnClickListener(l);b.setStateListAnimator(null);return b;
    }

    @Override Button segment(String s,boolean selected,View.OnClickListener l){
        Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setTextColor(selected?Color.WHITE:theme().muted);b.setBackground(segmentBg(selected));b.setOnClickListener(l);b.setStateListAnimator(null);return b;
    }
    @Override GradientDrawable segmentBg(boolean selected){return round(selected?theme().accent:theme().surface2,14,selected?theme().accent2:theme().stroke,1);}
}

package br.com.takirastreadores.field;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.view.*;
import android.widget.*;
import java.util.*;

public class InstallationActivityV70 extends InstallationActivityV61 {
    @Override public void onCreate(Bundle b){super.onCreate(b);replace70(list);}

    void replace70(View v){
        if(v instanceof TextView){TextView t=(TextView)v;String s=String.valueOf(t.getText());if(s.contains("TAKI FIELD • V6.1"))t.setText(s.replace("TAKI FIELD • V6.1","TAKI FIELD • V7.0"));if(s.contains("EVIDÊNCIAS FOTOGRÁFICAS"))t.setText("FOTOS DO ATENDIMENTO • OPCIONAL");if(s.contains("Registre o painel no início e o resultado final. Fotos extras entram no mesmo relatório."))t.setText("Adicione fotos somente quando forem úteis. Nenhuma foto é obrigatória para concluir o atendimento.");}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)replace70(g.getChildAt(i));}
    }

    @Override Button button(String s,boolean red,View.OnClickListener l){Button b=super.button(s,red,null);b.setOnClickListener(v->{feedback(false);if(l!=null)l.onClick(v);});return b;}
    @Override Button segment(String s,boolean selected,View.OnClickListener l){Button b=super.segment(s,selected,null);b.setOnClickListener(v->{feedback(false);if(l!=null)l.onClick(v);});return b;}
    void feedback(boolean important){try{if(appPrefs==null)appPrefs=getSharedPreferences("taki",MODE_PRIVATE);if(appPrefs.getBoolean("haptic_feedback_v70",true))getWindow().getDecorView().performHapticFeedback(important?HapticFeedbackConstants.LONG_PRESS:HapticFeedbackConstants.KEYBOARD_TAP);if(appPrefs.getBoolean("sound_feedback_v70",false)){AudioManager a=(AudioManager)getSystemService(AUDIO_SERVICE);if(a!=null)a.playSoundEffect(important?AudioManager.FX_KEYPRESS_RETURN:AudioManager.FX_KEY_CLICK,0.35f);}}catch(Exception ignored){}}

    // V7: fotos são evidências opcionais; nunca bloqueiam a geração do relatório.
    @Override void generateIndividual(){String p=cleanPlate();if(p.isEmpty()){plate.setError("Informe a placa");return;}feedback(true);generateSinglePdf(captureCurrent());}
    @Override void finishCurrentPlate(){String p=cleanPlate();if(p.isEmpty()){plate.setError("Informe a placa");return;}feedback(true);commitCurrentPlate();}
    @Override void finalizeBatchRequest(){
        if(!cleanPlate().isEmpty()){new AlertDialog.Builder(this).setTitle("Placa atual ainda aberta").setMessage("Deseja incluir "+cleanPlate()+" no lote antes de gerar o PDF?").setNegativeButton("Não incluir",(d,w)->generateBatchPdf()).setPositiveButton("INCLUIR E GERAR",(d,w)->{batchRecords.add(captureCurrent());clearCurrent();saveDraft(false);generateBatchPdf();}).show();return;}generateBatchPdf();
    }

    @Override void updateBatchStatus(){if(batchStatus==null)return;int current=batchRecords.size()+1;batchStatus.setText(batchRecords.size()+" placas concluídas  •  atual #"+current);batchStatus.setTextColor(batchRecords.isEmpty()?themeMuted():theme().accent);}

    @Override void showBatchList(){
        if(batchRecords.isEmpty()){Toast.makeText(this,"O lote ainda não tem placas finalizadas",Toast.LENGTH_SHORT).show();return;}
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(6),dp(12),dp(6));
        ScrollView scroll=new ScrollView(this);scroll.addView(box);
        for(int i=0;i<batchRecords.size();i++){final int index=i;ServiceRecord r=batchRecords.get(i);LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(dp(10),dp(7),dp(10),dp(7));TextView n=txt(String.format(Locale.getDefault(),"%02d",i+1),12,theme().accent,true);line.addView(n,new LinearLayout.LayoutParams(dp(38),dp(44)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(txt(r.plate,14,themeText(),true));info.addView(txt(r.service+(r.module.isEmpty()?"":" • "+r.module),11,themeMuted(),false));line.addView(info,new LinearLayout.LayoutParams(0,-2,1));Button edit=button("EDITAR",false,v->editBatchRecord(index));line.addView(edit,new LinearLayout.LayoutParams(dp(82),dp(42)));box.addView(line);}
        new AlertDialog.Builder(this).setTitle("LOTE • "+batchRecords.size()+" placas").setView(scroll).setNegativeButton("Fechar",null).show();
    }

    void editBatchRecord(int index){
        if(index<0||index>=batchRecords.size())return;ServiceRecord r=batchRecords.remove(index);applyCurrent(r);saveDraft(false);updateBatchStatus();Toast.makeText(this,r.plate+" aberta para edição. Finalize a placa para adicioná-la novamente ao lote.",Toast.LENGTH_LONG).show();
    }
}

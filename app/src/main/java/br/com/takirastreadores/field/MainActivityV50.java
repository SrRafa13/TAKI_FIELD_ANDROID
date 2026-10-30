package br.com.takirastreadores.field;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivityV50 extends MainActivityV30 {
    static final String QUANTA_VENDOR="Quanta";
    static final String VERSION_50="5.0.0";

    @Override void seed(){
        super.seed();
        String[] q={"Quanta"};
        add(QUANTA_VENDOR,
                "Reset",
                "Comando JSON informado pela equipe para reset do equipamento.",
                "{\n  \"operation\": \"command\",\n  \"args\": null,\n  \"data\": {\n    \"resetCan\": true\n  }\n}",
                q);
        add(QUANTA_VENDOR,
                "Reset da CAM",
                "Comando JSON informado pela equipe para reset da câmera.",
                "{ \"operation\": \"command\", \"args\": null, \"data\": { \"resetCan\": \"true\" } }",
                q);
    }

    @Override void home(){
        super.home();
        vendor.setAdapter(adapter(new String[]{"Suntech","X3Tech","Queclink",RS232_VENDOR,QUANTA_VENDOR}));

        LinearLayout ops=card();
        LinearLayout top=row();
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(txt("TAKI FIELD 5.0",16,WHITE,true));
        tx.addView(txt("FIELD OPS • RELATÓRIOS • COMANDOS",10,RED,true));
        top.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
        TextView live=txt("● OPERACIONAL",10,GREEN,true);live.setGravity(Gravity.END);top.addView(live);ops.addView(top,mb(8));
        ops.addView(txt("Central técnica renovada com relatórios em lote, retomada de sessão, Quanta e atalhos de operação.",12,MUTED,false),mb(10));
        File draft=new File(getFilesDir(),"installation_batch_v5.json");
        if(draft.exists()){
            LinearLayout resume=row();resume.addView(txt("Rascunho de lote salvo no aparelho",12,Color.rgb(255,190,80),true),new LinearLayout.LayoutParams(0,-2,1));resume.addView(button("RETOMAR",true,v->startActivity(new Intent(this,InstallationActivity.class))),new LinearLayout.LayoutParams(dp(104),dp(42)));ops.addView(resume);
        }
        list.addView(ops,1,mb(12));
    }

    @Override String[] models(String v){if(QUANTA_VENDOR.equals(v))return new String[]{"Quanta"};return super.models(v);}

    @Override View cmdCard(Cmd c){
        if(!QUANTA_VENDOR.equals(c.vendor))return super.cmdCard(c);
        LinearLayout box=card();
        LinearLayout top=row();top.addView(txt(c.title,16,WHITE,true),new LinearLayout.LayoutParams(0,-2,1));TextView tag=txt("JSON",10,Color.rgb(255,190,80),true);tag.setGravity(Gravity.CENTER);tag.setBackground(round(Color.rgb(45,37,22),99,Color.rgb(95,72,28),1));top.addView(tag,new LinearLayout.LayoutParams(dp(56),dp(30)));box.addView(top,mb(5));
        box.addView(txt(c.desc,12,MUTED,false),mb(8));TextView payload=txt(c.template,13,Color.rgb(255,190,80),false);payload.setTypeface(Typeface.MONOSPACE);payload.setTextIsSelectable(true);payload.setPadding(dp(12),dp(10),dp(12),dp(10));payload.setBackground(round(Color.rgb(11,12,14),12,Color.rgb(47,49,55),1));box.addView(payload,mb(10));
        box.addView(button("COPIAR COMANDO JSON",true,v->copy(c.template)),new LinearLayout.LayoutParams(-1,dp(48)));return box;
    }

    @Override boolean dangerous(String s){return super.dangerous(s)||s.contains("\"resetCan\"");}

    @Override void toolsScreen(){
        backHeader("FERRAMENTAS TAKI","Portais e utilitários usados em campo, reunidos em um só lugar.");
        tool("SETUP COMCEPT","https://setup.concept.inf.br/login");
        tool("PLATAFORMA TAKI","https://app.takirastreadores.com.br/app");
        tool("VIRTUEYES • DISPOSITIVOS","https://veye.com.br/veye/dispositivos");
        tool("WYZE • WEB VIEW","https://my.wyze.com/live");
        tool("GOOGLE MAPS","https://maps.google.com");
        tool("SPEEDTEST","https://www.speedtest.net");
        tool("REPOSITÓRIO / VERSÕES","https://github.com/SrRafa13/TAKI_FIELD_ANDROID");
        LinearLayout b=card();b.addView(txt("ATALHO DE RELATÓRIOS",14,WHITE,true),mb(5));b.addView(txt("Abra direto a rotina Individual/Em lote sem voltar à tela inicial.",12,MUTED,false),mb(9));b.addView(button("ABRIR INSTALAÇÕES",true,v->startActivity(new Intent(this,InstallationActivity.class))),new LinearLayout.LayoutParams(-1,dp(48)));list.addView(b,mtWrap(12));
        list.addView(button("VERIFICAR ATUALIZAÇÃO",true,v->checkUpdate()),mt(12));
    }

    @Override void checkUpdate(){
        Toast.makeText(this,"Consultando versão...",Toast.LENGTH_SHORT).show();
        new Thread(()->{try{
            URL u=new URL("https://raw.githubusercontent.com/SrRafa13/TAKI_FIELD_ANDROID/main/version.txt");
            BufferedReader br=new BufferedReader(new InputStreamReader(u.openStream()));String latest=br.readLine();br.close();
            runOnUiThread(()->new AlertDialog.Builder(this).setTitle("TAKI FIELD • Atualização").setMessage("Versão instalada: "+VERSION_50+"\nVersão disponível: "+latest+(VERSION_50.equals(latest)?"\n\nSeu app está atualizado.":"\n\nHá uma versão diferente disponível.")).setNegativeButton("Fechar",null).setPositiveButton("ABRIR VERSÕES",(d,w)->openUrl("https://github.com/SrRafa13/TAKI_FIELD_ANDROID/actions")).show());
        }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Não foi possível consultar atualização",Toast.LENGTH_LONG).show());}}).start();
    }

    @Override LinearLayout card(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(22,23,27),Color.rgb(12,13,16)});g.setCornerRadius(dp(18));g.setStroke(dp(1),Color.rgb(39,41,48));x.setBackground(g);x.setElevation(dp(3));return x;}
    @Override EditText input(String hint){EditText e=super.input(hint);e.setBackground(round(Color.rgb(25,26,30),13,Color.rgb(48,50,58),1));e.setPadding(dp(14),0,dp(14),0);return e;}
    @Override Button button(String s,boolean red,View.OnClickListener l){Button b=super.button(s,red,l);GradientDrawable g;if(red)g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{RED,Color.rgb(203,22,38)});else g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(38,39,45),Color.rgb(27,28,33)});g.setCornerRadius(dp(14));g.setStroke(dp(1),red?Color.rgb(255,65,78):Color.rgb(56,58,66));b.setBackground(g);b.setStateListAnimator(null);return b;}
    @Override Spinner spinner(String[] a){Spinner s=super.spinner(a);s.setBackground(round(Color.rgb(25,26,30),13,Color.rgb(48,50,58),1));return s;}

    GradientDrawable round(int color,int radius,int strokeColor,int stroke){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));if(stroke>0)g.setStroke(dp(stroke),strokeColor);return g;}
    LinearLayout.LayoutParams mtWrap(int x){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(x);return p;}
}

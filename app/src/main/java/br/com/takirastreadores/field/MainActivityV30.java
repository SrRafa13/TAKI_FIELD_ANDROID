package br.com.takirastreadores.field;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

public class MainActivityV30 extends MainActivityV21 {
    static final String CURRENT_VERSION = "3.0.0";
    boolean bypassDanger = false;

    @Override void home(){
        super.home();
        LinearLayout hub = card();
        hub.addView(txt("CENTRAL TÉCNICA",15,WHITE,true),mb(4));
        hub.addView(txt("Atalhos para atendimento, diagnóstico e documentação.",12,MUTED,false),mb(10));

        LinearLayout r1=row();
        r1.addView(button("INSTALAÇÕES",true,v->startActivity(new Intent(this,InstallationActivity.class))),new LinearLayout.LayoutParams(0,dp(50),1));
        gap(r1); r1.addView(button("DIAGNÓSTICO",false,v->diagnosticsScreen()),new LinearLayout.LayoutParams(0,dp(50),1)); hub.addView(r1,mb(8));
        LinearLayout r2=row();
        r2.addView(button("FAVORITOS",false,v->favoritesScreen()),new LinearLayout.LayoutParams(0,dp(50),1));
        gap(r2); r2.addView(button("HISTÓRICO",false,v->historyScreen()),new LinearLayout.LayoutParams(0,dp(50),1)); hub.addView(r2,mb(8));
        LinearLayout r3=row();
        r3.addView(button("PERFIS",false,v->profilesScreen()),new LinearLayout.LayoutParams(0,dp(50),1));
        gap(r3); r3.addView(button("INTERPRETAR",false,v->parserScreen()),new LinearLayout.LayoutParams(0,dp(50),1)); hub.addView(r3,mb(8));
        LinearLayout r4=row();
        r4.addView(button("LER ETIQUETA",false,v->scanLabel()),new LinearLayout.LayoutParams(0,dp(50),1));
        gap(r4); r4.addView(button("MANUAL",false,v->manualScreen()),new LinearLayout.LayoutParams(0,dp(50),1)); hub.addView(r4,mb(8));
        LinearLayout r5=row();
        r5.addView(button("FERRAMENTAS",false,v->toolsScreen()),new LinearLayout.LayoutParams(0,dp(50),1));
        gap(r5); r5.addView(button("CONFIGURAÇÕES",false,v->settingsScreen()),new LinearLayout.LayoutParams(0,dp(50),1)); hub.addView(r5);
        list.addView(hub,5,mb(12));
    }

    void gap(LinearLayout r){ Space s=new Space(this); r.addView(s,new LinearLayout.LayoutParams(dp(8),1)); }

    String cmdKey(Cmd c){ return c.vendor+"|"+c.title+"|"+c.template; }
    Set<String> favs(){ return new HashSet<>(prefs.getStringSet("favorites",new HashSet<>())); }
    void toggleFav(Cmd c){ Set<String> s=favs(); String k=cmdKey(c); if(s.contains(k))s.remove(k);else s.add(k);prefs.edit().putStringSet("favorites",s).apply();render(); }

    @Override View cmdCard(Cmd c){
        LinearLayout box=card();
        LinearLayout top=row(); TextView title=txt(c.title,16,WHITE,true);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button star=button(favs().contains(cmdKey(c))?"★":"☆",false,v->toggleFav(c));star.setTextSize(19);top.addView(star,new LinearLayout.LayoutParams(dp(52),dp(44)));box.addView(top);
        box.addView(txt(c.desc,12,MUTED,false),mb(7));
        TextView preview=txt(resolve(c.template),13,Color.rgb(255,190,80),false);preview.setTypeface(Typeface.MONOSPACE);preview.setTextIsSelectable(true);box.addView(preview,mb(10));
        if(RS232_VENDOR.equals(c.vendor)){
            box.addView(button("COPIAR COMANDO RS232",true,v->copy(resolve(c.template))),new LinearLayout.LayoutParams(-1,dp(46)));return box;
        }
        LinearLayout r=row();r.addView(button(c.fields.length>0?"GERAR":"COPIAR",true,v->{if(c.fields.length>0)generate(c);else copy(resolve(c.template));}),new LinearLayout.LayoutParams(0,dp(46),1));gap(r);r.addView(button("SMS",false,v->{if(c.fields.length>0)generateForSms(c);else sms(resolve(c.template));}),new LinearLayout.LayoutParams(0,dp(46),1));box.addView(r);return box;
    }

    @Override void copy(String s){
        if(!bypassDanger && dangerous(s)){
            new AlertDialog.Builder(this).setTitle("Confirmar comando").setMessage("Este comando pode alterar rede, servidor, bloqueio ou protocolo do equipamento. Confira os parâmetros antes de copiar.\n\n"+s).setNegativeButton("Cancelar",null).setPositiveButton("Copiar mesmo",(d,w)->{bypassDanger=true;copy(s);bypassDanger=false;}).show();return;
        }
        super.copy(s); addHistory(s);
    }
    @Override void sms(String s){ addHistory("SMS: "+s); super.sms(s); }
    boolean dangerous(String s){String x=s.toUpperCase(Locale.ROOT);return x.contains("APN")||x.contains("SERVER")||x.contains("RELAY")||x.contains("10;05#")||x.contains("817|8|")||x.contains("711|0|");}
    void addHistory(String s){String old=prefs.getString("history","");String ts=new SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new Date());String line=ts+"\t"+s.replace("\n"," ");String next=line+(old.isEmpty()?"":"\n"+old);String[] a=next.split("\n");if(a.length>60)next=String.join("\n",Arrays.copyOf(a,60));prefs.edit().putString("history",next).apply();}

    void backHeader(String title,String sub){list.removeAllViews();TextView b=txt("‹  VOLTAR",13,RED,true);b.setOnClickListener(v->home());list.addView(b,mb(12));list.addView(txt(title,23,WHITE,true),mb(4));if(sub!=null)list.addView(txt(sub,13,MUTED,false),mb(14));}

    void favoritesScreen(){
        backHeader("FAVORITOS","Seus comandos mais usados ficam aqui.");Set<String>s=favs();int n=0;
        for(Cmd c:all){if(!s.contains(cmdKey(c)))continue;n++;LinearLayout box=card();box.addView(txt(c.vendor+" • "+c.title,15,WHITE,true),mb(5));TextView p=txt(resolve(c.template),13,Color.rgb(255,190,80),false);p.setTypeface(Typeface.MONOSPACE);box.addView(p,mb(9));box.addView(button("ABRIR COMANDO",true,v->openCommand(c)),new LinearLayout.LayoutParams(-1,dp(46)));list.addView(box,mb(9));}
        if(n==0)list.addView(txt("Nenhum favorito ainda. Toque em ☆ em qualquer comando.",14,MUTED,false));
    }
    void openCommand(Cmd c){home();int pos=0;for(int i=0;i<vendor.getCount();i++)if(vendor.getItemAtPosition(i).toString().equals(c.vendor)){pos=i;break;}vendor.setSelection(pos);search.setText(c.title);}

    void historyScreen(){
        backHeader("HISTÓRICO","Últimos comandos copiados ou preparados para SMS.");String h=prefs.getString("history","");if(h.isEmpty()){list.addView(txt("Histórico vazio.",14,MUTED,false));return;}
        for(String line:h.split("\n")){String[] p=line.split("\t",2);LinearLayout box=card();box.addView(txt(p[0],11,MUTED,true),mb(5));TextView t=txt(p.length>1?p[1]:line,13,WHITE,false);t.setTypeface(Typeface.MONOSPACE);box.addView(t,mb(8));final String val=p.length>1?p[1]:line;box.addView(button("COPIAR NOVAMENTE",false,v->{bypassDanger=true;copy(val.replace("SMS: ",""));bypassDanger=false;}),new LinearLayout.LayoutParams(-1,dp(44)));list.addView(box,mb(8));}
        list.addView(button("LIMPAR HISTÓRICO",true,v->{prefs.edit().remove("history").apply();historyScreen();}),mt(10));
    }

    void diagnosticsScreen(){
        backHeader("DIAGNÓSTICO GUIADO","Escolha o sintoma e o app leva aos comandos mais úteis.");
        diag("SEM COMUNICAÇÃO","1. Consultar rede/sinal\n2. Conferir APN\n3. Conferir servidor/IP/porta\n4. Reiniciar módulo\n5. Validar novamente na plataforma","rede");
        diag("SEM GPS / POSIÇÃO","1. Solicitar posição\n2. Conferir fix GPS/status\n3. Reiniciar GPS quando disponível\n4. Verificar local de instalação/antena","posição");
        diag("IGNIÇÃO NÃO RECONHECE","1. Conferir pós-chave físico\n2. Verificar modo de ignição configurado\n3. Testar tensão/acelerômetro conforme instalação","ignição");
        diag("NÃO BLOQUEIA","1. Conferir saída/relé\n2. Testar comando de bloqueio\n3. Conferir instalação elétrica com segurança","bloquear");
        diag("RS232 / GETSCALE","1. Conferir protocolo atual\n2. Testar identificação nas macros\n3. Ajustar protocolo apenas após confirmar periférico","protocolo");
    }
    void diag(String name,String steps,String query){LinearLayout b=card();b.addView(txt(name,15,WHITE,true),mb(6));b.addView(txt(steps,13,MUTED,false),mb(10));b.addView(button("BUSCAR COMANDOS",false,v->{home();search.setText(query);}),new LinearLayout.LayoutParams(-1,dp(44)));list.addView(b,mb(9));}

    void profilesScreen(){
        backHeader("PERFIS RÁPIDOS","Sequências geradas com os dados locais. Confira antes de enviar; não são perfis oficiais do fabricante.");
        profile("SUNTECH • PÓS-CHAVE",()->buildSuntechProfile("01"));
        profile("SUNTECH • ACELERÔMETRO",()->buildSuntechProfile("03"));
        profile("X3TECH • BÁSICO",this::buildX3Profile);
    }
    interface StrMaker{String make();}
    void profile(String name,StrMaker maker){LinearLayout b=card();b.addView(txt(name,15,WHITE,true),mb(8));b.addView(button("GERAR E COPIAR",true,v->{bypassDanger=true;copy(maker.make());bypassDanger=false;}),new LinearLayout.LayoutParams(-1,dp(46)));list.addView(b,mb(9));}
    String buildSuntechProfile(String ign){String id=prefs.getString("id","XXX"),apn=prefs.getString("cfg_apn","APN"),u=prefs.getString("cfg_user",""),pw=prefs.getString("cfg_pass",""),srv=prefs.getString("cfg_server","IP"),port=prefs.getString("cfg_port","PORTA");return "PRG;"+id+";17;00#"+ign+"\nPRG;"+id+";10;00#01;01#"+apn+";02#"+u+";03#"+pw+"\nPRG;"+id+";10;05#"+srv+";06#"+port;}
    String buildX3Profile(){String apn=prefs.getString("cfg_apn","APN"),u=prefs.getString("cfg_user",""),pw=prefs.getString("cfg_pass",""),srv=prefs.getString("cfg_server","IP"),port=prefs.getString("cfg_port","PORTA");return "APN,"+apn+","+u+","+pw+"#\nSERVER,8520,"+srv+","+port+"#\nPARAM#\nSTATUS#";}

    void parserScreen(){
        backHeader("INTERPRETAR RESPOSTA","Cole a resposta recebida do rastreador. A leitura é heurística e deve ser conferida com o manual.");
        EditText e=input("Cole a resposta aqui");e.setSingleLine(false);e.setMinLines(6);e.setGravity(Gravity.TOP);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);list.addView(e,mb(10));
        TextView out=txt("",13,WHITE,false);
        list.addView(button("INTERPRETAR",true,v->out.setText(interpret(e.getText().toString()))),new LinearLayout.LayoutParams(-1,dp(48)));
        list.addView(out,mt(10));
    }
    String interpret(String raw){if(raw.trim().isEmpty())return "Cole uma resposta primeiro.";String x=raw.toUpperCase(Locale.ROOT);StringBuilder o=new StringBuilder("LEITURA RÁPIDA\n\n");Matcher m=Pattern.compile("(?:CSQ|RSSI)[:=, ]+(\\d+)").matcher(x);if(m.find()){int n=Integer.parseInt(m.group(1));o.append("Rede/sinal: ").append(n>=20?"boa":"atenção").append(" (").append(n).append(")\n");}if(x.contains("GPS:1")||x.contains("GPS=1")||x.contains("FIX:1"))o.append("GPS: posição/fix indicado como ativo\n");if(x.contains("GPS:0")||x.contains("GPS=0")||x.contains("FIX:0"))o.append("GPS: sem fix indicado\n");if(x.contains("ACC:1")||x.contains("IGN:1")||x.contains("IGNITION:1"))o.append("Ignição: ON\n");if(x.contains("ACC:0")||x.contains("IGN:0")||x.contains("IGNITION:0"))o.append("Ignição: OFF\n");if(x.contains("ICCID"))o.append("ICCID: presente na resposta\n");if(x.contains("IMEI"))o.append("IMEI: presente na resposta\n");o.append("\nResposta original:\n").append(raw);return o.toString();}

    void manualScreen(){
        backHeader("MANUAL INTELIGENTE","Pesquisa unificada por função em Suntech, X3Tech, Queclink e RS232.");EditText q=input("Ex.: buzzer, APN, reboot, ignição...");list.addView(q,mb(10));LinearLayout holder=new LinearLayout(this);holder.setOrientation(LinearLayout.VERTICAL);list.addView(holder);
        Runnable r=()->{holder.removeAllViews();String s=q.getText().toString().toLowerCase(Locale.ROOT);for(Cmd c:all){String hay=(c.vendor+" "+c.title+" "+c.desc+" "+c.template).toLowerCase(Locale.ROOT);if(!s.isEmpty()&&!hay.contains(s))continue;LinearLayout b=card();b.addView(txt(c.vendor+" • "+c.title,14,WHITE,true),mb(4));b.addView(txt(c.desc,12,MUTED,false),mb(5));TextView t=txt(resolve(c.template),12,Color.rgb(255,190,80),false);t.setTypeface(Typeface.MONOSPACE);b.addView(t);holder.addView(b,mb(8));}};q.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){r.run();}public void afterTextChanged(Editable e){}});r.run();
    }

    void toolsScreen(){
        backHeader("FERRAMENTAS TAKI","Acesso rápido sem procurar links no navegador.");
        tool("SETUP", "https://setup.concept.inf.br/login");tool("PLATAFORMA TAKI","https://app.takirastreadores.com.br/app");tool("GOOGLE MAPS","https://maps.google.com");tool("SPEEDTEST","https://www.speedtest.net");tool("REPOSITÓRIO / VERSÕES","https://github.com/SrRafa13/TAKI_FIELD_ANDROID");
        list.addView(button("VERIFICAR ATUALIZAÇÃO",true,v->checkUpdate()),mt(12));
    }
    void tool(String name,String url){list.addView(button(name,false,v->openUrl(url)),mb(8));}

    void settingsScreen(){
        backHeader("CONFIGURAÇÕES DA EMPRESA","Salvas somente neste aparelho. Use para gerar perfis sem deixar credenciais no código público.");
        EditText op=input("Operadora / perfil");op.setText(prefs.getString("cfg_operator",""));list.addView(op,mb(8));
        EditText apn=input("APN");apn.setText(prefs.getString("cfg_apn",""));list.addView(apn,mb(8));
        EditText usr=input("Usuário APN");usr.setText(prefs.getString("cfg_user",""));list.addView(usr,mb(8));
        EditText pass=input("Senha APN");pass.setText(prefs.getString("cfg_pass",""));list.addView(pass,mb(8));
        EditText srv=input("Servidor / IP");srv.setText(prefs.getString("cfg_server",""));list.addView(srv,mb(8));
        EditText port=input("Porta");port.setText(prefs.getString("cfg_port",""));list.addView(port,mb(8));
        EditText qpass=input("Senha Queclink padrão");qpass.setText(prefs.getString("cfg_qlpass",""));list.addView(qpass,mb(10));
        list.addView(button("SALVAR CONFIGURAÇÕES",true,v->{prefs.edit().putString("cfg_operator",op.getText().toString().trim()).putString("cfg_apn",apn.getText().toString().trim()).putString("cfg_user",usr.getText().toString().trim()).putString("cfg_pass",pass.getText().toString()).putString("cfg_server",srv.getText().toString().trim()).putString("cfg_port",port.getText().toString().trim()).putString("cfg_qlpass",qpass.getText().toString()).apply();Toast.makeText(this,"Configurações salvas localmente",Toast.LENGTH_SHORT).show();}),new LinearLayout.LayoutParams(-1,dp(50)));
    }

    void scanLabel(){
        try{GmsBarcodeScanning.getClient(this).startScan().addOnSuccessListener(barcode->{String v=barcode.getRawValue();if(v!=null&&!v.trim().isEmpty()){idField.setText(v.trim());Toast.makeText(this,"Número lido da etiqueta",Toast.LENGTH_SHORT).show();}}).addOnFailureListener(e->Toast.makeText(this,"Não foi possível ler a etiqueta",Toast.LENGTH_LONG).show());}
        catch(Exception e){Toast.makeText(this,"Leitor indisponível neste aparelho",Toast.LENGTH_LONG).show();}
    }

    void checkUpdate(){
        Toast.makeText(this,"Consultando versão...",Toast.LENGTH_SHORT).show();new Thread(()->{try{URL u=new URL("https://raw.githubusercontent.com/SrRafa13/TAKI_FIELD_ANDROID/main/version.txt");BufferedReader br=new BufferedReader(new InputStreamReader(u.openStream()));String latest=br.readLine();br.close();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Atualização").setMessage("Versão instalada: "+CURRENT_VERSION+"\nVersão disponível: "+latest+(CURRENT_VERSION.equals(latest)?"\n\nSeu app está atualizado.":"\n\nHá uma versão diferente no repositório.")).setNegativeButton("Fechar",null).setPositiveButton("Abrir versões",(d,w)->openUrl("https://github.com/SrRafa13/TAKI_FIELD_ANDROID/actions")).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Não foi possível consultar atualização",Toast.LENGTH_LONG).show());}}).start();
    }
}

package br.com.takirastreadores.field;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int BG=Color.rgb(8,8,8), PANEL=Color.rgb(17,17,17), PANEL2=Color.rgb(25,25,25), RED=Color.rgb(245,34,45), WHITE=Color.rgb(246,246,246), MUTED=Color.rgb(160,160,160), GREEN=Color.rgb(53,199,89);
    LinearLayout root, list;
    Spinner vendor, model;
    EditText idField, phoneField, search;
    TextView status;
    android.content.SharedPreferences prefs;
    final List<Cmd> all = new ArrayList<>();

    static class Cmd {
        String vendor, title, desc, template;
        String[] models, fields, hints;
        Cmd(String v,String t,String d,String c,String[] m,String[] f,String[] h){vendor=v;title=t;desc=d;template=c;models=m;fields=f;hints=h;}
    }

    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("taki",MODE_PRIVATE); seed(); build(); }

    void build(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        ScrollView sc=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(16),dp(12),dp(16),dp(32)); sc.addView(list);
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root); home();
    }

    void home(){
        list.removeAllViews();
        LinearLayout head=row();
        LinearLayout brand=new LinearLayout(this); brand.setOrientation(LinearLayout.VERTICAL);
        TextView t=txt("TAKI FIELD",24,WHITE,true); brand.addView(t);
        brand.addView(txt("TERMINAL TÉCNICO • OFFLINE",11,MUTED,true));
        head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView dot=txt("● PRONTO",11,GREEN,true); dot.setGravity(Gravity.END); head.addView(dot);
        list.addView(head,mb(16));

        LinearLayout selector=card();
        selector.addView(label("FABRICANTE"));
        vendor=spinner(new String[]{"Suntech","X3Tech","Queclink"}); selector.addView(vendor,mb(10));
        selector.addView(label("MODELO")); model=spinner(models("Suntech")); selector.addView(model,mb(10));
        idField=input("ESN / IMEI do equipamento"); idField.setText(prefs.getString("id","")); selector.addView(idField,mb(8));
        phoneField=input("Número do chip (opcional)"); phoneField.setInputType(3); phoneField.setText(prefs.getString("phone","")); selector.addView(phoneField);
        list.addView(selector,mb(12));

        search=input("🔎  Buscar: reset, APN, velocidade..."); list.addView(search,mb(12));
        LinearLayout quick=row(); quick.addView(button("CHECKLIST",false,v->checklist()),new LinearLayout.LayoutParams(0,dp(48),1));
        Space sp=new Space(this); quick.addView(sp,new LinearLayout.LayoutParams(dp(8),1));
        quick.addView(button("LIMPAR BUSCA",false,v->{search.setText("");}),new LinearLayout.LayoutParams(0,dp(48),1)); list.addView(quick,mb(14));
        status=txt("",12,MUTED,false); list.addView(status,mb(8));
        LinearLayout commandHolder=new LinearLayout(this); commandHolder.setId(9876); commandHolder.setOrientation(LinearLayout.VERTICAL); list.addView(commandHolder);

        vendor.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){String x=vendor.getSelectedItem().toString(); model.setAdapter(adapter(models(x))); render();}});
        model.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){render();}});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){} public void onTextChanged(CharSequence s,int a,int b,int c){render();} public void afterTextChanged(Editable e){}});
        idField.addTextChangedListener(saveWatcher("id")); phoneField.addTextChangedListener(saveWatcher("phone"));
        render();
    }

    TextWatcher saveWatcher(String key){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int d){}public void onTextChanged(CharSequence s,int a,int b,int c){prefs.edit().putString(key,s.toString().trim()).apply();}public void afterTextChanged(Editable e){}};}

    void render(){
        if(vendor==null||model==null)return; LinearLayout h=list.findViewById(9876); if(h==null)return; h.removeAllViews();
        String v=vendor.getSelectedItem().toString(), m=model.getSelectedItem()==null?"":model.getSelectedItem().toString(), q=search.getText().toString().toLowerCase(Locale.ROOT).trim(); int n=0;
        for(Cmd c:all){ if(!c.vendor.equals(v)||!supports(c,m))continue; String hay=(c.title+" "+c.desc+" "+c.template).toLowerCase(Locale.ROOT); if(!q.isEmpty()&&!hay.contains(q))continue; h.addView(cmdCard(c),mb(10)); n++; }
        status.setText(n+" comandos • "+v+" • "+m);
    }

    boolean supports(Cmd c,String m){ if(c.models==null||c.models.length==0)return true; for(String x:c.models)if(x.equals(m))return true; return false; }

    View cmdCard(Cmd c){
        LinearLayout box=card(); TextView title=txt(c.title,16,WHITE,true); box.addView(title); box.addView(txt(c.desc,12,MUTED,false),mb(7));
        TextView preview=txt(c.template,13,Color.rgb(255,190,80),false); preview.setTypeface(Typeface.MONOSPACE); preview.setTextIsSelectable(true); box.addView(preview,mb(10));
        LinearLayout r=row(); r.addView(button(c.fields.length>0?"GERAR":"COPIAR",true,v->{ if(c.fields.length>0)generate(c); else copy(resolve(c.template)); }),new LinearLayout.LayoutParams(0,dp(46),1));
        Space sp=new Space(this); r.addView(sp,new LinearLayout.LayoutParams(dp(8),1)); r.addView(button("SMS",false,v->{ if(c.fields.length>0)generateForSms(c); else sms(resolve(c.template));}),new LinearLayout.LayoutParams(0,dp(46),1)); box.addView(r); return box;
    }

    void generate(Cmd c){ generator(c,false); } void generateForSms(Cmd c){ generator(c,true); }
    void generator(Cmd c,boolean send){
        LinearLayout p=new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setPadding(dp(20),dp(8),dp(20),0); final EditText[] inputs=new EditText[c.fields.length];
        for(int i=0;i<c.fields.length;i++){p.addView(txt(c.fields[i],12,MUTED,true)); inputs[i]=input(c.hints.length>i?c.hints[i]:c.fields[i]); p.addView(inputs[i],mb(10));}
        new AlertDialog.Builder(this).setTitle(c.title).setView(p).setNegativeButton("Cancelar",null).setPositiveButton(send?"Abrir SMS":"Copiar",(d,w)->{String s=resolve(c.template); for(int i=0;i<inputs.length;i++)s=s.replace("{"+i+"}",inputs[i].getText().toString().trim()); if(send)sms(s); else copy(s);}).show();
    }

    String resolve(String s){String id=idField==null?"XXXX":idField.getText().toString().trim(); if(id.isEmpty())id="XXXX"; return s.replace("XXXX",id);}
    void copy(String s){((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("TAKI FIELD",s)); Toast.makeText(this,"Comando copiado",Toast.LENGTH_SHORT).show();}
    void sms(String s){String p=phoneField.getText().toString().trim(); Uri u=p.isEmpty()?Uri.parse("smsto:"):Uri.parse("smsto:"+Uri.encode(p)); Intent i=new Intent(Intent.ACTION_SENDTO,u);i.putExtra("sms_body",s);try{startActivity(i);}catch(Exception e){copy(s);Toast.makeText(this,"SMS indisponível; comando copiado",Toast.LENGTH_LONG).show();}}

    void checklist(){
        list.removeAllViews(); TextView back=txt("‹  VOLTAR",13,RED,true); list.addView(back,mb(12)); back.setOnClickListener(v->home()); list.addView(txt("CHECKLIST DE INSTALAÇÃO",23,WHITE,true),mb(6)); list.addView(txt("Marque antes de encerrar o atendimento.",13,MUTED,false),mb(14));
        String[] items={"Alimentação e negativo conferidos","Pós-chave / ignição validada","Módulo fixado e oculto corretamente","GPS com posição válida","Rede / GPRS / LTE online","APN e servidor conferidos","Bloqueio testado com segurança","Entrada / saída testadas","IMEI / ESN registrado","Teste final na plataforma"};
        for(String x:items){CheckBox cb=new CheckBox(this);cb.setText(x);cb.setTextColor(WHITE);cb.setTextSize(15);cb.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{RED,MUTED}));list.addView(cb,mb(6));}
        list.addView(button("GERAR RESUMO",true,v->summary()),mt(14));
    }
    void summary(){copy("TAKI FIELD — CHECKLIST DE INSTALAÇÃO\nEquipamento: "+prefs.getString("id","-")+"\nAtendimento finalizado e testes conferidos.");}

    String[] models(String v){ if(v.equals("Suntech"))return new String[]{"ST8210","ST8310","ST8310R","ST8310W","Suntech 4G"}; if(v.equals("X3Tech"))return new String[]{"XT40","XT40-OBDII","NT40 CAT-1","NT40-P4G","NT40-OBDII CAT-1"}; return new String[]{"GV350M Series"}; }

    void seed(){
        String[] S={"ST8210","ST8310","ST8310R","ST8310W","Suntech 4G"}, X={"XT40","XT40-OBDII","NT40 CAT-1","NT40-P4G","NT40-OBDII CAT-1"}, O={"XT40-OBDII","NT40-OBDII CAT-1"}, Q={"GV350M Series"};
        add("Suntech","Reiniciar módulo","Reboot sem restaurar parâmetros.","CMD;XXXX;03;03",S);
        add("Suntech","Solicitar posição","Solicita posição/status atual.","CMD;XXXX;03;01",S);
        add("Suntech","Requisitar configuração","Consulta configuração atual.","CMD;XXXX;03;05",S);
        add("Suntech","Versão de firmware","Consulta firmware.","CMD;XXXX;03;04;1",S);
        add("Suntech","Consultar ICCID","Consulta ICCID do SIM.","CMD;XXXX;01;03",S);
        add("Suntech","Ignição física / pós-chave","Usa entrada física de ignição.","PRG;XXXX;17;00#01",S);
        add("Suntech","Ignição virtual por tensão","Usa tensão para determinar ignição.","PRG;XXXX;17;00#02",S);
        add("Suntech","Ignição virtual por acelerômetro","Usa movimento para determinar ignição.","PRG;XXXX;17;00#03",S);
        gen("Suntech","Limite de velocidade","Define limite em km/h.","PRG;XXXX;16;21#{0}",S,new String[]{"Velocidade (km/h)"},new String[]{"100"});
        gen("Suntech","Tensão alta e baixa","Limiares da ignição virtual por tensão.","PRG;XXXX;17;15#{0};16#{1}",S,new String[]{"Tensão alta","Tensão baixa"},new String[]{"13.50","12.50"});
        gen("Suntech","APN","Configura APN, usuário e senha.","PRG;XXXX;10;00#01;01#{0};02#{1};03#{2}",S,new String[]{"APN","Usuário","Senha"},new String[]{"apn","usuario","senha"});
        gen("Suntech","IP e porta","Configura servidor principal e secundário.","PRG;XXXX;10;05#{0};06#{1};08#{2};09#{3}",S,new String[]{"IP principal","Porta","IP secundário","Porta 2"},new String[]{"0.0.0.0","0000","0.0.0.0","0000"});
        add("Suntech","TCP","Seleciona protocolo TCP.","PRG;XXXX;10;07#00;10#00",S); add("Suntech","UDP","Seleciona protocolo UDP.","PRG;XXXX;10;07#01;10#01",S);
        add("Suntech","Ativar saída 1","Aciona saída 1.","CMD;XXXX;04;01",S); add("Suntech","Desativar saída 1","Desaciona saída 1.","CMD;XXXX;04;02",S);
        add("Suntech","Sleep desabilitado","Consumo normal.","PRG;XXXX;19;30#00",S); add("Suntech","Ultra-baixo consumo","Desliga GPS e GPRS em sleep.","PRG;XXXX;19;30#01",S); add("Suntech","Baixo consumo","Desliga somente GPS.","PRG;XXXX;19;30#02",S);

        gen("X3Tech","APN","Configura APN, usuário e senha.","APN,{0},{1},{2}#",X,new String[]{"APN","Usuário","Senha"},new String[]{"apn","usuario","senha"});
        gen("X3Tech","Servidor IP / porta","Código 8520 é fixo conforme manual.","SERVER,8520,{0},{1}#",X,new String[]{"IP / DNS","Porta"},new String[]{"0.0.0.0","0000"});
        gen("X3Tech","Tempo em movimento","Intervalo de upload em segundos.","TIMER,{0}#",X,new String[]{"Segundos"},new String[]{"60"});
        add("X3Tech","Dormir após ignição OFF","Ativa economia de energia.","SLPON#",X); add("X3Tech","Desativar sleep","Mantém operação sem economia.","SLPOFF#",X);
        gen("X3Tech","Tempo até dormir","Minutos após ignição OFF.","SLEEP,{0}#",X,new String[]{"Minutos"},new String[]{"3"}); gen("X3Tech","Ciclo de wake","Horas para acordar parado.","WAKE,{0}#",X,new String[]{"Horas"},new String[]{"12"});
        add("X3Tech","Parâmetros","Consulta parâmetros do rastreador.","PARAM#",X); add("X3Tech","Status","Consulta sensores, tensão, GPS e ignição.","STATUS#",X); add("X3Tech","Rede","Consulta rede LTE/GSM e sinal.","NETWORK#",X); add("X3Tech","Firmware","Consulta versão do firmware.","VERSION#",X);
        add("X3Tech","Localização","Solicita localização atual.","WHERE#",X); add("X3Tech","Link Google Maps","Retorna URL de localização.","URL#",X);
        add("X3Tech","Reiniciar rastreador","Reinicia o equipamento.","RESET#",X); add("X3Tech","Reiniciar GPS","Reinicia módulo GPS.","CLRG#",X);
        add("X3Tech","Bloquear","Aciona relé de bloqueio.","RELAY,1#",X); add("X3Tech","Desbloquear","Libera relé de bloqueio.","RELAY,0#",X);
        gen("X3Tech","Limite de velocidade","Velocidade e intervalo de alerta.","SPEEDING,{0},{1}#",X,new String[]{"km/h","Intervalo"},new String[]{"100","3"});
        gen("X3Tech","Sensor de vibração","1 = mais sensível; 5 = menos sensível.","VIBRATION,{0}#",X,new String[]{"Sensibilidade 1-5"},new String[]{"3"});
        add("X3Tech","Ignição virtual ON","Habilita ignição virtual.","ACCVIRTON#",X); add("X3Tech","Ignição virtual OFF","Desabilita ignição virtual.","ACCVIRTOFF#",X);
        gen("X3Tech","Delay ignição virtual OFF","Tempo parado até desligar ignição virtual.","DELAYACC,{0}#",X,new String[]{"Segundos"},new String[]{"120"});
        gen("X3Tech","Aceleração brusca","Alarme de driver behavior OBD.","EACCELE,{0},{1}#",O,new String[]{"Limiar","Modo 0-3"},new String[]{"5","0"}); gen("X3Tech","Frenagem brusca","Alarme de driver behavior OBD.","EDECELE,{0},{1}#",O,new String[]{"Limiar","Modo 0-3"},new String[]{"5","0"}); gen("X3Tech","Curva brusca","Alarme de driver behavior OBD.","EANGLE,{0},{1}#",O,new String[]{"Limiar","Modo 0-3"},new String[]{"5","0"});

        gen("Queclink","APN LTE","AT+GTBSI — configura APN LTE/GPRS.","AT+GTBSI={0},{1},{2},{3},,,,,0,2,0,FFFF$",Q,new String[]{"Senha do equipamento","APN","Usuário APN","Senha APN"},new String[]{"gv350m","apn","",""});
        gen("Queclink","Servidor principal","AT+GTSRI — servidor e modo de reporte.","AT+GTSRI={0},{1},,1,{2},{3},,,,{4},1,,,,,0001$",Q,new String[]{"Senha","Report mode 0-7","IP / domínio","Porta","Heartbeat min"},new String[]{"gv350m","3","0.0.0.0","0000","15"});
        gen("Queclink","Posição imediata","RTO subcomando 1 = localização atual.","AT+GTRTO={0},1,,,,,,0001$",Q,new String[]{"Senha"},new String[]{"gv350m"});
        gen("Queclink","Reiniciar","RTO subcomando 3 = reboot.","AT+GTRTO={0},3,,,,,,0001$",Q,new String[]{"Senha"},new String[]{"gv350m"});
        gen("Queclink","Consultar ICCID","RTO subcomando 6.","AT+GTRTO={0},6,,,,,,0001$",Q,new String[]{"Senha"},new String[]{"gv350m"});
        gen("Queclink","Consultar sinal","RTO subcomando 7 = CSQ.","AT+GTRTO={0},7,,,,,,0001$",Q,new String[]{"Senha"},new String[]{"gv350m"});
        gen("Queclink","Consultar versão","RTO subcomando 8.","AT+GTRTO={0},8,,,,,,0001$",Q,new String[]{"Senha"},new String[]{"gv350m"});
        gen("Queclink","Consultar bateria","RTO subcomando 9.","AT+GTRTO={0},9,,,,,,0001$",Q,new String[]{"Senha"},new String[]{"gv350m"});
        gen("Queclink","Consultar I/O","RTO subcomando A.","AT+GTRTO={0},A,,,,,,0001$",Q,new String[]{"Senha"},new String[]{"gv350m"});
    }

    void add(String v,String t,String d,String c,String[] m){all.add(new Cmd(v,t,d,c,m,new String[]{},new String[]{}));}
    void gen(String v,String t,String d,String c,String[] m,String[] f,String[] h){all.add(new Cmd(v,t,d,c,m,f,h));}

    LinearLayout card(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(16),dp(15),dp(16),dp(15));x.setBackgroundResource(R.drawable.bg_card);return x;}
    LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);x.setGravity(Gravity.CENTER_VERTICAL);return x;}
    TextView txt(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    TextView label(String s){TextView v=txt(s,11,MUTED,true);v.setLetterSpacing(.08f);return v;}
    EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(105,105,105));e.setTextColor(WHITE);e.setTextSize(15);e.setSingleLine(true);e.setBackgroundResource(R.drawable.bg_input);e.setPadding(dp(14),0,dp(14),0);e.setMinHeight(dp(52));return e;}
    Button button(String s,boolean red,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setTextColor(WHITE);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setBackgroundResource(red?R.drawable.bg_button_red:R.drawable.bg_button_dark);b.setOnClickListener(l);return b;}
    Spinner spinner(String[] a){Spinner s=new Spinner(this);s.setAdapter(adapter(a));s.setBackgroundResource(R.drawable.bg_input);s.setPadding(dp(10),0,dp(10),0);s.setMinimumHeight(dp(52));return s;}
    android.widget.ArrayAdapter<String> adapter(String[] a){android.widget.ArrayAdapter<String> ad=new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,a){@Override public View getView(int p,View c,android.view.ViewGroup g){TextView v=(TextView)super.getView(p,c,g);v.setTextColor(WHITE);v.setTextSize(15);return v;}@Override public View getDropDownView(int p,View c,android.view.ViewGroup g){TextView v=(TextView)super.getDropDownView(p,c,g);v.setTextColor(WHITE);v.setBackgroundColor(PANEL2);v.setPadding(dp(16),dp(14),dp(16),dp(14));return v;}};return ad;}
    LinearLayout.LayoutParams mb(int x){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(x);return p;} LinearLayout.LayoutParams mt(int x){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.topMargin=dp(x);return p;} int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
}

package br.com.takirastreadores.field;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

final class BackupStore {
    static final class Result {
        int reports;
        boolean history;
    }

    static Result exportZip(Context c, Uri target, SharedPreferences prefs) throws Exception {
        Result result = new Result();
        try (OutputStream raw = c.getContentResolver().openOutputStream(target);
             ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(raw))) {
            putBytes(zip, "prefs.json", prefsJson(prefs).toString(2).getBytes("UTF-8"));

            File history = new File(c.getFilesDir(), ServiceStore.FILE_NAME);
            if (history.exists()) {
                putFile(zip, history, "data/" + ServiceStore.FILE_NAME);
                result.history = true;
            }

            File reports = new File(c.getFilesDir(), "reports");
            File[] files = reports.listFiles();
            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File f : files) if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    putFile(zip, f, "reports/" + f.getName());
                    result.reports++;
                }
            }
        }
        return result;
    }

    static Result importZip(Context c, Uri source, SharedPreferences prefs) throws Exception {
        Result result = new Result();
        File tmp = new File(c.getCacheDir(), "restore_61");
        deleteTree(tmp); tmp.mkdirs();
        try (InputStream raw = c.getContentResolver().openInputStream(source);
             ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
            ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = zip.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String name = e.getName().replace('\\','/');
                if (!(name.equals("prefs.json") || name.equals("data/" + ServiceStore.FILE_NAME) || name.startsWith("reports/"))) continue;
                File out = new File(tmp, name);
                String root = tmp.getCanonicalPath() + File.separator;
                if (!out.getCanonicalPath().startsWith(root)) throw new IOException("Backup inválido");
                File parent = out.getParentFile(); if (parent != null) parent.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    int n; while ((n = zip.read(buf)) > 0) fos.write(buf,0,n);
                }
            }
        }

        File prefsFile = new File(tmp, "prefs.json");
        if (!prefsFile.exists()) throw new IOException("Backup não contém preferências");
        JSONObject p = new JSONObject(readText(prefsFile));
        restorePrefs(prefs, p);
        prefs.edit().remove("theme_preview").remove("pending_shared_location").apply();

        File history = new File(tmp, "data/" + ServiceStore.FILE_NAME);
        if (history.exists()) {
            copy(history, new File(c.getFilesDir(), ServiceStore.FILE_NAME));
            result.history = true;
        }

        File incomingReports = new File(tmp, "reports");
        File reports = new File(c.getFilesDir(), "reports"); reports.mkdirs();
        File[] r = incomingReports.listFiles();
        if (r != null) for (File f : r) if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            copy(f, new File(reports, f.getName())); result.reports++;
        }
        deleteTree(tmp);
        return result;
    }

    private static JSONObject prefsJson(SharedPreferences prefs) throws Exception {
        JSONObject root = new JSONObject();
        root.put("format", "TAKI_FIELD_BACKUP");
        root.put("version", 61);
        JSONObject values = new JSONObject();
        for (Map.Entry<String,?> e : prefs.getAll().entrySet()) {
            if ("theme_preview".equals(e.getKey()) || "pending_shared_location".equals(e.getKey())) continue;
            Object v = e.getValue();
            JSONObject item = new JSONObject();
            if (v instanceof String) { item.put("type","string"); item.put("value",v); }
            else if (v instanceof Boolean) { item.put("type","boolean"); item.put("value",v); }
            else if (v instanceof Integer) { item.put("type","int"); item.put("value",v); }
            else if (v instanceof Long) { item.put("type","long"); item.put("value",v); }
            else if (v instanceof Float) { item.put("type","float"); item.put("value",((Float)v).doubleValue()); }
            else if (v instanceof Set) {
                item.put("type","stringset"); JSONArray a = new JSONArray();
                for (Object x : (Set<?>)v) a.put(String.valueOf(x)); item.put("value",a);
            } else continue;
            values.put(e.getKey(), item);
        }
        root.put("prefs", values);
        return root;
    }

    private static void restorePrefs(SharedPreferences prefs, JSONObject root) throws Exception {
        if (!"TAKI_FIELD_BACKUP".equals(root.optString("format"))) throw new IOException("Arquivo não é um backup do TAKI FIELD");
        JSONObject values = root.optJSONObject("prefs"); if (values == null) throw new IOException("Preferências ausentes");
        SharedPreferences.Editor ed = prefs.edit().clear();
        Iterator<String> keys = values.keys();
        while (keys.hasNext()) {
            String key = keys.next(); JSONObject item = values.optJSONObject(key); if (item == null) continue;
            String type = item.optString("type");
            if ("string".equals(type)) ed.putString(key,item.optString("value",""));
            else if ("boolean".equals(type)) ed.putBoolean(key,item.optBoolean("value",false));
            else if ("int".equals(type)) ed.putInt(key,item.optInt("value",0));
            else if ("long".equals(type)) ed.putLong(key,item.optLong("value",0));
            else if ("float".equals(type)) ed.putFloat(key,(float)item.optDouble("value",0));
            else if ("stringset".equals(type)) {
                LinkedHashSet<String> set = new LinkedHashSet<>(); JSONArray a=item.optJSONArray("value");
                if(a!=null) for(int i=0;i<a.length();i++) set.add(a.optString(i)); ed.putStringSet(key,set);
            }
        }
        ed.apply();
    }

    private static void putBytes(ZipOutputStream zip,String name,byte[] bytes)throws Exception{
        zip.putNextEntry(new ZipEntry(name)); zip.write(bytes); zip.closeEntry();
    }
    private static void putFile(ZipOutputStream zip,File file,String name)throws Exception{
        zip.putNextEntry(new ZipEntry(name)); byte[] b=new byte[8192];
        try(FileInputStream in=new FileInputStream(file)){int n;while((n=in.read(b))>0)zip.write(b,0,n);} zip.closeEntry();
    }
    private static String readText(File f)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];try(FileInputStream in=new FileInputStream(f)){int n;while((n=in.read(buf))>0)b.write(buf,0,n);}return new String(b.toByteArray(),"UTF-8");}
    private static void copy(File a,File b)throws Exception{File p=b.getParentFile();if(p!=null)p.mkdirs();try(FileInputStream in=new FileInputStream(a);FileOutputStream out=new FileOutputStream(b)){byte[] x=new byte[8192];int n;while((n=in.read(x))>0)out.write(x,0,n);}}
    private static void deleteTree(File f){if(f==null||!f.exists())return;if(f.isDirectory()){File[] a=f.listFiles();if(a!=null)for(File x:a)deleteTree(x);}f.delete();}
    private BackupStore(){}
}

package br.com.takirastreadores.field;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

final class ServiceStore {
    static final String FILE_NAME = "service_history_v6.json";

    static class Entry {
        String plate = "";
        String service = "";
        String brand = "";
        String module = "";
        String technician = "";
        String finishedAt = "";
        String batchId = "";
        String reportFile = "";
        String reportType = "";
        int reportCount = 0;
        long epoch = 0L;

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("plate", plate);
                o.put("service", service);
                o.put("brand", brand);
                o.put("module", module);
                o.put("technician", technician);
                o.put("finishedAt", finishedAt);
                o.put("batchId", batchId);
                o.put("reportFile", reportFile);
                o.put("reportType", reportType);
                o.put("reportCount", reportCount);
                o.put("epoch", epoch);
            } catch (Exception ignored) {}
            return o;
        }

        static Entry fromJson(JSONObject o) {
            Entry e = new Entry();
            e.plate = o.optString("plate", "");
            e.service = o.optString("service", "");
            e.brand = o.optString("brand", "");
            e.module = o.optString("module", "");
            e.technician = o.optString("technician", "");
            e.finishedAt = o.optString("finishedAt", "");
            e.batchId = o.optString("batchId", "");
            e.reportFile = o.optString("reportFile", "");
            e.reportType = o.optString("reportType", e.batchId.isEmpty() ? "individual" : "batch");
            e.reportCount = o.optInt("reportCount", e.batchId.isEmpty() ? 1 : 0);
            e.epoch = o.optLong("epoch", 0L);
            return e;
        }

        String key() {
            return (plate + "|" + service + "|" + technician + "|" + finishedAt).toUpperCase(Locale.ROOT);
        }
    }

    static synchronized ArrayList<Entry> list(Context c) {
        ArrayList<Entry> out = new ArrayList<>();
        try {
            File f = new File(c.getFilesDir(), FILE_NAME);
            if (!f.exists()) return out;
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            try (FileInputStream in = new FileInputStream(f)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            }
            JSONObject root = new JSONObject(new String(b.toByteArray(), "UTF-8"));
            JSONArray arr = root.optJSONArray("entries");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o != null) out.add(Entry.fromJson(o));
                }
            }
        } catch (Exception ignored) {}
        Collections.sort(out, new Comparator<Entry>() {
            @Override public int compare(Entry a, Entry b) { return Long.compare(b.epoch, a.epoch); }
        });
        return out;
    }

    static synchronized void add(Context c, Entry incoming) {
        ArrayList<Entry> entries = list(c);
        String key = incoming.key();
        for (Entry e : entries) {
            if (e.key().equals(key)) {
                if (!incoming.reportFile.isEmpty() && e.reportFile.isEmpty()) {
                    e.reportFile = incoming.reportFile;
                    e.reportType = incoming.reportType;
                    e.reportCount = incoming.reportCount;
                    write(c, entries);
                }
                return;
            }
        }
        entries.add(0, incoming);
        if (entries.size() > 1500) entries = new ArrayList<>(entries.subList(0, 1500));
        write(c, entries);
    }

    static synchronized void add(Context c, String plate, String service, String brand, String module,
                                 String technician, String finishedAt, long epoch, String batchId) {
        add(c, plate, service, brand, module, technician, finishedAt, epoch, batchId, "", batchId == null || batchId.isEmpty() ? "individual" : "batch", batchId == null || batchId.isEmpty() ? 1 : 0);
    }

    static synchronized void add(Context c, String plate, String service, String brand, String module,
                                 String technician, String finishedAt, long epoch, String batchId,
                                 String reportFile, String reportType, int reportCount) {
        Entry e = new Entry();
        e.plate = plate == null ? "" : plate;
        e.service = service == null ? "" : service;
        e.brand = brand == null ? "" : brand;
        e.module = module == null ? "" : module;
        e.technician = technician == null ? "" : technician;
        e.finishedAt = finishedAt == null ? "" : finishedAt;
        e.epoch = epoch;
        e.batchId = batchId == null ? "" : batchId;
        e.reportFile = reportFile == null ? "" : reportFile;
        e.reportType = reportType == null ? "" : reportType;
        e.reportCount = Math.max(0, reportCount);
        add(c, e);
    }

    static synchronized void remove(Context c, Entry target) {
        ArrayList<Entry> entries = list(c);
        String key = target.key();
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).key().equals(key)) entries.remove(i);
        }
        write(c, entries);
    }

    static synchronized void clear(Context c) {
        try { new File(c.getFilesDir(), FILE_NAME).delete(); } catch (Exception ignored) {}
    }

    static synchronized void write(Context c, ArrayList<Entry> entries) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 2);
            JSONArray arr = new JSONArray();
            for (Entry e : entries) arr.put(e.toJson());
            root.put("entries", arr);
            File target = new File(c.getFilesDir(), FILE_NAME);
            File tmp = new File(c.getFilesDir(), FILE_NAME + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(root.toString().getBytes("UTF-8"));
            }
            if (target.exists()) target.delete();
            if (!tmp.renameTo(target)) {
                try (FileOutputStream out = new FileOutputStream(target)) {
                    out.write(root.toString().getBytes("UTF-8"));
                }
                tmp.delete();
            }
        } catch (Exception ignored) {}
    }

    private ServiceStore() {}
}

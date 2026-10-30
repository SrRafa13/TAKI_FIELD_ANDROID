package br.com.takirastreadores.field;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivityV611 extends MainActivityV61 {
    static final String VERSION_611 = "6.1.1";
    Calendar dashboardMonthCursor;

    Calendar dashboardCursor() {
        if (dashboardMonthCursor == null) {
            dashboardMonthCursor = Calendar.getInstance();
            dashboardMonthCursor.set(Calendar.DAY_OF_MONTH, 1);
            dashboardMonthCursor.set(Calendar.HOUR_OF_DAY, 12);
            dashboardMonthCursor.set(Calendar.MINUTE, 0);
            dashboardMonthCursor.set(Calendar.SECOND, 0);
            dashboardMonthCursor.set(Calendar.MILLISECOND, 0);
        }
        return dashboardMonthCursor;
    }

    @Override void screenHeader(String title, String sub, String active) {
        super.screenHeader(title, sub, active);
        replaceVersionLabel(list);
    }

    @Override void welcomeScreen() {
        super.welcomeScreen();
        replaceVersionLabel(list);
    }

    void replaceVersionLabel(View view) {
        if (view instanceof TextView) {
            TextView t = (TextView) view;
            String s = String.valueOf(t.getText());
            if (s.contains("FIELD OPS • 6.1")) t.setText(s.replace("FIELD OPS • 6.1", "FIELD OPS • 6.1.1"));
            if (s.contains("FIELD OPERATIONS • 6.1")) t.setText(s.replace("FIELD OPERATIONS • 6.1", "FIELD OPERATIONS • 6.1.1"));
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) replaceVersionLabel(g.getChildAt(i));
        }
    }

    @Override void dashboardScreen() {
        screenHeader("DASHBOARD", "Calendário sincronizado com a data do aparelho e navegação livre entre meses.", "DASHBOARD");
        ArrayList<ServiceStore.Entry> entries = ServiceStore.list(this);
        Calendar cursor = dashboardCursor();

        LinearLayout period = card();
        period.addView(label("MÊS DO CALENDÁRIO"), mb(7));
        LinearLayout monthRow = row();
        Button previous = button("‹", false, null);
        TextView monthTitle = txt(monthTitle(cursor), 16, themeText(), true);
        monthTitle.setGravity(Gravity.CENTER);
        Button next = button("›", false, null);
        monthRow.addView(previous, new LinearLayout.LayoutParams(dp(54), dp(48)));
        gap(monthRow);
        monthRow.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(48), 1));
        gap(monthRow);
        monthRow.addView(next, new LinearLayout.LayoutParams(dp(54), dp(48)));
        period.addView(monthRow, mb(8));

        Calendar deviceNow = Calendar.getInstance();
        TextView deviceDate = txt("HOJE • " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(deviceNow.getTime()) + " • data do aparelho", 11, themeMuted(), false);
        deviceDate.setGravity(Gravity.CENTER);
        period.addView(deviceDate, mb(8));
        Button today = button("VOLTAR PARA O MÊS ATUAL", false, null);
        period.addView(today, new LinearLayout.LayoutParams(-1, dp(44)));
        list.addView(period, mb(11));

        LinearLayout filters = card();
        filters.addView(label("TIPO DE SERVIÇO"));
        Spinner service = spinner(new String[]{"Todos", "Instalação", "Troca", "Retirada", "Manutenção"});
        filters.addView(service, mb(8));
        filters.addView(label("TÉCNICO"));
        Spinner tech = spinner(dashboardTechFilters(entries));
        filters.addView(tech);
        list.addView(filters, mb(11));

        LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        list.addView(holder);

        final Runnable[] render = new Runnable[1];
        render[0] = () -> {
            monthTitle.setText(monthTitle(cursor));
            renderDashboard611(holder, entries, cursor, service.getSelectedItem().toString(), tech.getSelectedItem().toString());
        };

        previous.setOnClickListener(v -> {
            cursor.add(Calendar.MONTH, -1);
            render[0].run();
        });
        next.setOnClickListener(v -> {
            cursor.add(Calendar.MONTH, 1);
            render[0].run();
        });
        today.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            cursor.setTimeInMillis(now.getTimeInMillis());
            cursor.set(Calendar.DAY_OF_MONTH, 1);
            cursor.set(Calendar.HOUR_OF_DAY, 12);
            cursor.set(Calendar.MINUTE, 0);
            cursor.set(Calendar.SECOND, 0);
            cursor.set(Calendar.MILLISECOND, 0);
            render[0].run();
        });

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { render[0].run(); }
        };
        service.setOnItemSelectedListener(listener);
        tech.setOnItemSelectedListener(listener);
        render[0].run();
    }

    String monthTitle(Calendar c) {
        String s = new SimpleDateFormat("MMMM yyyy", new Locale("pt", "BR")).format(c.getTime());
        return s.toUpperCase(new Locale("pt", "BR"));
    }

    void renderDashboard611(LinearLayout holder, ArrayList<ServiceStore.Entry> all, Calendar monthCursor, String service, String tech) {
        holder.removeAllViews();
        int wantedMonth = monthCursor.get(Calendar.MONTH);
        int wantedYear = monthCursor.get(Calendar.YEAR);
        ArrayList<ServiceStore.Entry> filtered = new ArrayList<>();

        Calendar ec = Calendar.getInstance();
        for (ServiceStore.Entry e : all) {
            if (e.epoch <= 0) continue;
            ec.setTimeInMillis(e.epoch);
            if (ec.get(Calendar.MONTH) != wantedMonth || ec.get(Calendar.YEAR) != wantedYear) continue;
            if (!"Todos".equals(service) && !service.equals(e.service)) continue;
            if (!"Todos os técnicos".equals(tech) && !tech.equals(e.technician)) continue;
            filtered.add(e);
        }

        ArrayList<ServiceStore.Entry> installations = new ArrayList<>();
        LinkedHashSet<String> plates = new LinkedHashSet<>();
        for (ServiceStore.Entry e : filtered) {
            if ("Instalação".equals(e.service)) installations.add(e);
            if (e.plate != null && !e.plate.trim().isEmpty()) plates.add(e.plate);
        }

        String title = monthTitle(monthCursor);
        LinearLayout metrics = row();
        metrics.addView(metricCard("SERVIÇOS", filtered.size(), v -> showEntriesDialog("Serviços • " + title, filtered)), new LinearLayout.LayoutParams(0, dp(92), 1));
        gap(metrics);
        metrics.addView(metricCard("INSTALAÇÕES", installations.size(), v -> showEntriesDialog("Instalações • " + title, installations)), new LinearLayout.LayoutParams(0, dp(92), 1));
        gap(metrics);
        metrics.addView(metricCard("PLACAS", plates.size(), v -> showEntriesDialog("Placas • " + title, filtered)), new LinearLayout.LayoutParams(0, dp(92), 1));
        holder.addView(metrics, mb(12));

        renderCalendar611(holder, monthCursor, filtered);
    }

    void renderCalendar611(LinearLayout holder, Calendar monthCursor, ArrayList<ServiceStore.Entry> filtered) {
        int year = monthCursor.get(Calendar.YEAR);
        int month = monthCursor.get(Calendar.MONTH);
        Calendar first = Calendar.getInstance();
        first.clear();
        first.set(year, month, 1, 12, 0, 0);
        int days = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int offset = first.get(Calendar.DAY_OF_WEEK) - 1;

        Calendar now = Calendar.getInstance();
        boolean viewingCurrentMonth = now.get(Calendar.YEAR) == year && now.get(Calendar.MONTH) == month;

        LinearLayout box = card();
        box.addView(txt("CALENDÁRIO • " + monthTitle(monthCursor), 14, themeText(), true), mb(3));
        TextView hint = txt(viewingCurrentMonth ? "O dia de hoje está destacado automaticamente pela data do celular." : "Use VOLTAR PARA O MÊS ATUAL para retornar à data do celular.", 11, themeMuted(), false);
        box.addView(hint, mb(10));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        String[] week = {"D", "S", "T", "Q", "Q", "S", "S"};
        for (String w : week) {
            TextView x = txt(w, 10, themeMuted(), true);
            x.setGravity(Gravity.CENTER);
            grid.addView(x, weightedCellParams(0));
        }
        for (int i = 0; i < offset; i++) {
            TextView blank = txt("", 10, themeMuted(), false);
            grid.addView(blank, weightedCellParams(2));
        }

        Calendar serviceCal = Calendar.getInstance();
        for (int day = 1; day <= days; day++) {
            final int selectedDay = day;
            ArrayList<ServiceStore.Entry> dayEntries = new ArrayList<>();
            for (ServiceStore.Entry e : filtered) {
                if (e.epoch <= 0) continue;
                serviceCal.setTimeInMillis(e.epoch);
                if (serviceCal.get(Calendar.YEAR) == year && serviceCal.get(Calendar.MONTH) == month && serviceCal.get(Calendar.DAY_OF_MONTH) == day) dayEntries.add(e);
            }

            boolean isToday = viewingCurrentMonth && now.get(Calendar.DAY_OF_MONTH) == day;
            String cellText;
            if (!dayEntries.isEmpty()) cellText = day + "\n" + dayEntries.size() + " serv.";
            else if (isToday) cellText = day + "\nHOJE";
            else cellText = String.valueOf(day);

            int textColor = isToday ? theme().accent : (dayEntries.isEmpty() ? themeMuted() : themeText());
            TextView cell = txt(cellText, (!dayEntries.isEmpty() || isToday) ? 10 : 11, textColor, isToday || !dayEntries.isEmpty());
            cell.setGravity(Gravity.CENTER);
            cell.setMinHeight(dp(56));
            int fill = isToday ? theme().surface : (dayEntries.isEmpty() ? theme().surface2 : theme().surface);
            int stroke = (isToday || !dayEntries.isEmpty()) ? theme().accent : theme().stroke;
            cell.setBackground(round(fill, 10, stroke, isToday ? 2 : 1));
            if (!dayEntries.isEmpty()) {
                cell.setClickable(true);
                cell.setOnClickListener(v -> showEntriesDialog(String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, month + 1, year), dayEntries));
            }
            grid.addView(cell, weightedCellParams(2));
        }
        box.addView(grid);
        holder.addView(box, mb(12));
    }

    GridLayout.LayoutParams weightedCellParams(int marginDp) {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        p.width = 0;
        p.setMargins(dp(marginDp), dp(marginDp), dp(marginDp), dp(marginDp));
        return p;
    }

    @Override void checkUpdate() {
        Toast.makeText(this, "Consultando versão...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                URL u = new URL("https://raw.githubusercontent.com/SrRafa13/TAKI_FIELD_ANDROID/main/version.txt");
                BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream()));
                String latest = br.readLine();
                br.close();
                runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("TAKI FIELD • Atualização")
                    .setMessage("Versão instalada: " + VERSION_611 + "\nVersão disponível: " + latest + (VERSION_611.equals(latest) ? "\n\nSeu app está atualizado." : "\n\nHá uma versão diferente disponível."))
                    .setNegativeButton("Fechar", null)
                    .setPositiveButton("ABRIR VERSÕES", (d, w) -> openUrl("https://github.com/SrRafa13/TAKI_FIELD_ANDROID/actions"))
                    .show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Não foi possível consultar atualização", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}

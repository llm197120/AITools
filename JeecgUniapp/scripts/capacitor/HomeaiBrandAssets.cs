using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;
using System.Runtime.InteropServices;

public static class HomeaiBrandAssets
{
    private const int LumThreshold = 140;
    private const double LauncherGlyphCover = 0.52;
    private const double SplashGlyphCover = 0.30;
    private static readonly Color AppCream = Color.FromArgb(0xF3, 0xF2, 0xEE);
    private static readonly Color FallbackFill = Color.FromArgb(0xED, 0xE5, 0xD9);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool DestroyIcon(IntPtr hIcon);

    public static string Generate(string srcPng, string androidResDir, string faviconPath)
    {
        if (string.IsNullOrEmpty(srcPng) || !File.Exists(srcPng))
        {
            throw new FileNotFoundException("HA source icon not found", srcPng);
        }

        Bitmap src;
        using (FileStream fs = File.OpenRead(srcPng))
        {
            src = new Bitmap(fs);
        }

        Bitmap glyph = null;
        try
        {
            Rectangle bbox;
            Color fill;
            ExtractGlyph(src, out glyph, out bbox, out fill);

            if (!string.IsNullOrEmpty(androidResDir))
            {
                WriteAndroid(androidResDir, glyph, fill);
            }
            if (!string.IsNullOrEmpty(faviconPath))
            {
                WriteFavicon(faviconPath, glyph, fill);
            }

            return string.Format(
                "fill=#{0:X2}{1:X2}{2:X2} glyph={3}x{4}",
                fill.R, fill.G, fill.B, bbox.Width, bbox.Height);
        }
        finally
        {
            if (glyph != null) glyph.Dispose();
            src.Dispose();
        }
    }

    private static void ExtractGlyph(Bitmap src, out Bitmap glyph, out Rectangle bbox, out Color fill)
    {
        int w = src.Width;
        int h = src.Height;
        Rectangle full = new Rectangle(0, 0, w, h);
        BitmapData data = src.LockBits(full, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        int[] px = new int[w * h];
        Marshal.Copy(data.Scan0, px, 0, px.Length);
        src.UnlockBits(data);

        long fr = 0, fg = 0, fb = 0, fn = 0;
        int minX = w, minY = h, maxX = -1, maxY = -1;
        bool[] isLogo = new bool[w * h];
        for (int i = 0; i < px.Length; i++)
        {
            int a = (px[i] >> 24) & 0xff;
            int r = (px[i] >> 16) & 0xff;
            int g = (px[i] >> 8) & 0xff;
            int b = px[i] & 0xff;
            if (a < 16) continue;
            double lum = 0.299 * r + 0.587 * g + 0.114 * b;
            if (lum < LumThreshold)
            {
                isLogo[i] = true;
                int x = i % w;
                int y = i / w;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
            else
            {
                fr += r;
                fg += g;
                fb += b;
                fn++;
            }
        }

        fill = fn == 0
            ? FallbackFill
            : Color.FromArgb((int)(fr / fn), (int)(fg / fn), (int)(fb / fn));

        if (maxX < minX)
        {
            glyph = new Bitmap(src);
            bbox = new Rectangle(0, 0, w, h);
            return;
        }

        minX = Math.Max(0, minX - 4);
        minY = Math.Max(0, minY - 4);
        maxX = Math.Min(w - 1, maxX + 4);
        maxY = Math.Min(h - 1, maxY + 4);
        bbox = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        glyph = new Bitmap(bbox.Width, bbox.Height, PixelFormat.Format32bppArgb);
        glyph.MakeTransparent();
        for (int y = 0; y < bbox.Height; y++)
        {
            for (int x = 0; x < bbox.Width; x++)
            {
                int i = (y + minY) * w + (x + minX);
                if (!isLogo[i]) continue;
                int p = px[i];
                glyph.SetPixel(x, y, Color.FromArgb(
                    (p >> 24) & 0xff,
                    (p >> 16) & 0xff,
                    (p >> 8) & 0xff,
                    p & 0xff));
            }
        }
    }

    private static void WriteAndroid(string resDir, Bitmap glyph, Color fill)
    {
        int[][] launcher = {
            new[] { 48, 108 },
            new[] { 72, 162 },
            new[] { 96, 216 },
            new[] { 144, 324 },
            new[] { 192, 432 }
        };
        string[] folders = { "mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi" };
        for (int i = 0; i < folders.Length; i++)
        {
            string dir = Path.Combine(resDir, folders[i]);
            Directory.CreateDirectory(dir);
            int legacy = launcher[i][0];
            int adaptive = launcher[i][1];
            SavePng(Path.Combine(dir, "ic_launcher.png"), MakeSquareIcon(glyph, fill, legacy, false));
            SavePng(Path.Combine(dir, "ic_launcher_round.png"), MakeSquareIcon(glyph, fill, legacy, true));
            SavePng(Path.Combine(dir, "ic_launcher_foreground.png"), MakeForeground(glyph, adaptive));
        }

        string anyDpi = Path.Combine(resDir, "mipmap-anydpi-v26");
        Directory.CreateDirectory(anyDpi);
        string adaptiveXml =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" +
            "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\r\n" +
            "    <background android:drawable=\"@color/ic_launcher_background\"/>\r\n" +
            "    <foreground android:drawable=\"@mipmap/ic_launcher_foreground\"/>\r\n" +
            "</adaptive-icon>\r\n";
        File.WriteAllText(Path.Combine(anyDpi, "ic_launcher.xml"), adaptiveXml);
        File.WriteAllText(Path.Combine(anyDpi, "ic_launcher_round.xml"), adaptiveXml);

        string values = Path.Combine(resDir, "values");
        Directory.CreateDirectory(values);
        string hex = string.Format("#{0:X2}{1:X2}{2:X2}", fill.R, fill.G, fill.B);
        File.WriteAllText(
            Path.Combine(values, "ic_launcher_background.xml"),
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n<resources>\r\n    <color name=\"ic_launcher_background\">" + hex + "</color>\r\n</resources>\r\n");
        File.WriteAllText(
            Path.Combine(values, "colors.xml"),
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" +
            "<resources>\r\n" +
            "    <color name=\"colorPrimary\">#1B4F8A</color>\r\n" +
            "    <color name=\"colorPrimaryDark\">#163E6C</color>\r\n" +
            "    <color name=\"colorAccent\">#1B4F8A</color>\r\n" +
            "    <color name=\"splash_background\">#F3F2EE</color>\r\n" +
            "</resources>\r\n");

        WriteSplash(resDir, glyph, "drawable", 480, 320);
        WriteSplash(resDir, glyph, "drawable-land-mdpi", 480, 320);
        WriteSplash(resDir, glyph, "drawable-land-hdpi", 800, 480);
        WriteSplash(resDir, glyph, "drawable-land-xhdpi", 1280, 720);
        WriteSplash(resDir, glyph, "drawable-land-xxhdpi", 1600, 960);
        WriteSplash(resDir, glyph, "drawable-land-xxxhdpi", 1920, 1280);
        WriteSplash(resDir, glyph, "drawable-port-mdpi", 320, 480);
        WriteSplash(resDir, glyph, "drawable-port-hdpi", 480, 800);
        WriteSplash(resDir, glyph, "drawable-port-xhdpi", 720, 1280);
        WriteSplash(resDir, glyph, "drawable-port-xxhdpi", 960, 1600);
        WriteSplash(resDir, glyph, "drawable-port-xxxhdpi", 1280, 1920);

        int[] notify = { 24, 36, 48, 72, 96 };
        string[] notifyDir = { "drawable-mdpi", "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi", "drawable-xxxhdpi" };
        for (int i = 0; i < notify.Length; i++)
        {
            string dir = Path.Combine(resDir, notifyDir[i]);
            Directory.CreateDirectory(dir);
            SavePng(Path.Combine(dir, "ic_stat_homeai.png"), MakeNotification(glyph, notify[i]));
        }

        string tealBg = Path.Combine(resDir, "drawable", "ic_launcher_background.xml");
        if (File.Exists(tealBg)) File.Delete(tealBg);
        string robot = Path.Combine(resDir, "drawable-v24", "ic_launcher_foreground.xml");
        if (File.Exists(robot)) File.Delete(robot);
    }

    private static void WriteSplash(string resDir, Bitmap glyph, string folder, int width, int height)
    {
        string dir = Path.Combine(resDir, folder);
        Directory.CreateDirectory(dir);
        using (Bitmap bmp = new Bitmap(width, height, PixelFormat.Format32bppArgb))
        using (Graphics g = Graphics.FromImage(bmp))
        {
            SetupGraphics(g);
            g.Clear(AppCream);
            int min = Math.Min(width, height);
            DrawGlyph(g, glyph, width, height, min * SplashGlyphCover);
            bmp.Save(Path.Combine(dir, "splash.png"), ImageFormat.Png);
        }
    }

    private static Bitmap MakeSquareIcon(Bitmap glyph, Color fill, int size, bool round)
    {
        Bitmap bmp = new Bitmap(size, size, PixelFormat.Format32bppArgb);
        using (Graphics g = Graphics.FromImage(bmp))
        {
            SetupGraphics(g);
            g.Clear(Color.Transparent);
            if (round)
            {
                using (SolidBrush brush = new SolidBrush(fill))
                using (GraphicsPath path = new GraphicsPath())
                {
                    path.AddEllipse(0, 0, size - 1, size - 1);
                    g.FillPath(brush, path);
                    g.SetClip(path);
                }
            }
            else
            {
                g.Clear(fill);
            }
            DrawGlyph(g, glyph, size, size, size * LauncherGlyphCover);
        }
        return bmp;
    }

    private static Bitmap MakeForeground(Bitmap glyph, int size)
    {
        Bitmap bmp = new Bitmap(size, size, PixelFormat.Format32bppArgb);
        using (Graphics g = Graphics.FromImage(bmp))
        {
            SetupGraphics(g);
            g.Clear(Color.Transparent);
            DrawGlyph(g, glyph, size, size, size * LauncherGlyphCover);
        }
        return bmp;
    }

    private static Bitmap MakeNotification(Bitmap glyph, int size)
    {
        Bitmap bmp = new Bitmap(size, size, PixelFormat.Format32bppArgb);
        using (Graphics g = Graphics.FromImage(bmp))
        using (ImageAttributes ia = WhiteGlyphAttributes())
        {
            SetupGraphics(g);
            g.Clear(Color.Transparent);
            int pad = Math.Max(2, size / 8);
            int inner = size - pad * 2;
            float scale = Math.Min((float)inner / glyph.Width, (float)inner / glyph.Height);
            int dw = Math.Max(1, (int)(glyph.Width * scale));
            int dh = Math.Max(1, (int)(glyph.Height * scale));
            int x = (size - dw) / 2;
            int y = (size - dh) / 2;
            g.DrawImage(
                glyph,
                new Rectangle(x, y, dw, dh),
                0, 0, glyph.Width, glyph.Height,
                GraphicsUnit.Pixel,
                ia);
        }
        return bmp;
    }

    private static ImageAttributes WhiteGlyphAttributes()
    {
        float[][] matrix = {
            new float[] { 0, 0, 0, 0, 0 },
            new float[] { 0, 0, 0, 0, 0 },
            new float[] { 0, 0, 0, 0, 0 },
            new float[] { 0, 0, 0, 1, 0 },
            new float[] { 1, 1, 1, 0, 1 }
        };
        ColorMatrix cm = new ColorMatrix(matrix);
        ImageAttributes ia = new ImageAttributes();
        ia.SetColorMatrix(cm);
        return ia;
    }

    private static void DrawGlyph(Graphics g, Bitmap glyph, int canvasW, int canvasH, double target)
    {
        float scale = Math.Min((float)target / glyph.Width, (float)target / glyph.Height);
        int dw = Math.Max(1, (int)(glyph.Width * scale));
        int dh = Math.Max(1, (int)(glyph.Height * scale));
        int x = (canvasW - dw) / 2;
        int y = (canvasH - dh) / 2;
        g.DrawImage(glyph, x, y, dw, dh);
    }

    private static void SetupGraphics(Graphics g)
    {
        g.InterpolationMode = InterpolationMode.HighQualityBicubic;
        g.PixelOffsetMode = PixelOffsetMode.HighQuality;
        g.SmoothingMode = SmoothingMode.HighQuality;
        g.CompositingQuality = CompositingQuality.HighQuality;
        g.CompositingMode = CompositingMode.SourceOver;
    }

    private static void SavePng(string path, Bitmap bmp)
    {
        try
        {
            bmp.Save(path, ImageFormat.Png);
        }
        finally
        {
            bmp.Dispose();
        }
    }

    private static void WriteFavicon(string path, Bitmap glyph, Color fill)
    {
        using (Bitmap bmp = MakeSquareIcon(glyph, fill, 32, false))
        {
            IntPtr hIcon = bmp.GetHicon();
            try
            {
                using (Icon icon = Icon.FromHandle(hIcon))
                using (FileStream fs = File.Create(path))
                {
                    icon.Save(fs);
                }
            }
            finally
            {
                DestroyIcon(hIcon);
            }
        }
    }
}

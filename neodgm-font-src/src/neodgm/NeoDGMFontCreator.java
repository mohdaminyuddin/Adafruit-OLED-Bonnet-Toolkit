/*
 * This file is part of the Adafruit OLED Bonnet Toolkit: a Java toolkit for the Adafruit 128x64 OLED bonnet,
 * with support for the screen, D-pad/buttons, UI layout, and task scheduling.
 *
 * Hosted at: https://github.com/lukehutch/Adafruit-OLED-Bonnet-Toolkit
 * 
 * This code is not associated with or endorsed by Adafruit. Adafruit is a trademark of Limor "Ladyada" Fried. 
*/
package neodgm;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Create NeoDGM font tables.
 * 
 * Converted by Luke Hutchison to Java, based on https://github.com/Dalgona/neodgm/blob/master/font.py , which
 * places the compiled TTF version of the font under the SIL Open Font License, but the source contains the comment
 * "Original font was released under the public domain by Jungtae Kim in 1990s. Conversion & additional character
 * design by Dalgona. <dalgona@hontou.moe>". The code doesn't have a separate license listed.
 */
public class NeoDGMFontCreator {

    private static final int[][] cho_tbl = { //
            { 0, 0, 0, 0, 0, 0, 0, 0, 1, 3, 3, 3, 1, 2, 4, 4, 4, 2, 1, 3, 0 }, //
            { 5, 5, 5, 5, 5, 5, 5, 5, 6, 7, 7, 7, 6, 6, 7, 7, 7, 6, 6, 7, 5 } };
    private static final int[] jung_tbl = { 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1 };
    private static final int[] jong_tbl = { 0, 2, 0, 2, 1, 2, 1, 2, 3, 0, 2, 1, 3, 3, 1, 2, 1, 3, 3, 1, 1 };

    private static final int NUM_ROWS = 16;
    private static final int NUM_BYTES_PER_COLUMN = (NUM_ROWS >> 3);

    private static final Map<Character, byte[]> charDefs = new TreeMap<>();
    private static final Map<String, byte[]> jamoDefs = new HashMap<>();

    private static byte[] getJamo(String jamoName) {
        byte[] bytes = jamoDefs.get(jamoName);
        if (bytes == null) {
            throw new RuntimeException("Jamo not found: " + jamoName);
        }
        return bytes;
    }

    private static void mergeChar(String jamoName, byte[] mergeInto) {
        byte[] bytes = getJamo(jamoName);
        if (bytes.length != mergeInto.length) {
            throw new RuntimeException("Length mismatch");
        }
        for (int i = 0; i < mergeInto.length; i++) {
            mergeInto[i] |= bytes[i];
        }
    }

    public static void main(String[] args) throws IOException {
        File srcDir = new File("neodgm-font-src");
        if (!srcDir.exists()) {
            throw new FileNotFoundException(srcDir.toString());
        }

        // Read .fnt files
        for (File f : srcDir.listFiles()) {
            if (f.getName().endsWith(".fnt")) {
                List<String> lines = Files.readAllLines(f.toPath());
                for (int i = 0; i < lines.size() - 1; i += NUM_ROWS + 1) {
                    String[] parts = lines.get(i).split(" ");
                    if (parts.length != 2 && parts.length != 3) {
                        throw new RuntimeException("Wrong number of fields");
                    }
                    int width = Integer.parseInt(parts[1]);
                    if (width != 8 && width != 16) {
                        throw new RuntimeException("Bad width");
                    }
                    byte[] bytes = new byte[width * NUM_BYTES_PER_COLUMN];
                    for (int r = 0; r < NUM_ROWS; r++) {
                        String line = lines.get(i + 1 + r);
                        for (int c = 0; c < width; c++) {
                            int bit = (line.charAt(c) - '0') & 1;
                            bytes[c * NUM_BYTES_PER_COLUMN + (r >> 3)] |= (bit << (r & 7));
                        }
                    }
                    if (parts.length == 2) {
                        // char
                        char charCode = (char) Integer.parseInt(parts[0]);
                        charDefs.put(charCode, bytes);
                    } else if (parts.length == 3) {
                        // jamo (hangul-jamo-source.fnt)
                        String jamoKey = parts[0];
                        int xoff = Integer.parseInt(parts[2]);
                        if (xoff != 0) {
                            throw new RuntimeException("Unsupported xoff: " + xoff);
                        }
                        jamoDefs.put(jamoKey, bytes);
                    }
                }
            }
        }

        // Hangul Choseong
        for (int i = 0; i < 19; i++) {
            charDefs.put((char) (0x1100 + i), getJamo("cho_" + i + "_0"));
        }

        // Hangul Jungseong
        for (int i = 0; i < 21; i++) {
            charDefs.put((char) (0x1161 + i), getJamo("jung_" + i + "_0"));
        }

        // Hangul Jongseong
        for (int i = 0; i < 27; i++) {
            charDefs.put((char) (0x11A8 + i), getJamo("jong_" + i + "_0"));
        }

        // Hangul syllables (11172 glyphs)
        for (int i = 0xAC00; i < 0xD7A4; i++) {
            byte[] bytes = charDefs.get((char) i);
            if (bytes == null) {
                charDefs.put((char) i, bytes = new byte[16 * NUM_BYTES_PER_COLUMN]);
            }
            int a = (i - 0xAC00) / (21 * 28);
            int b = ((i - 0xAC00) % (21 * 28)) / 28;
            int c = (i - 0xAC00) % 28;
            int x = cho_tbl[c > 0 ? 1 : 0][b];
            int y = jung_tbl[a] + (c > 0 ? 2 : 0);
            int z = jong_tbl[b];
            mergeChar("cho_" + a + "_" + x, bytes);
            mergeChar("jung_" + b + "_" + y, bytes);
            if (c != 0) {
                mergeChar("jong_" + c + "_" + z, bytes);
            }
        }

        // Pack all bytes
        int totBytes = 0;
        for (Entry<Character, byte[]> ent : charDefs.entrySet()) {
            byte[] bytes = ent.getValue();
            totBytes += bytes.length;
        }
        Map<Character, Integer> charToAllBytesIdx = new HashMap<>();
        byte[] allBytes = new byte[totBytes];
        int allBytesIdx = 0;
        for (Entry<Character, byte[]> ent : charDefs.entrySet()) {
            Character c = ent.getKey();
            charToAllBytesIdx.put(c, allBytesIdx);
            byte[] bytes = ent.getValue();
            for (byte b : bytes) {
                allBytes[allBytesIdx++] = b;
            }
        }

        // Output result
        new File("src/main/resources/fonts").mkdir();
        try (FileOutputStream fos = new FileOutputStream("src/main/resources/fonts/neodgm-font");
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                DataOutputStream out = new DataOutputStream(bos)) {
            int numChars = charDefs.size();
            out.writeInt(numChars);
            for (Entry<Character, byte[]> ent : charDefs.entrySet()) {
                Character c = ent.getKey();
                byte[] bytes = ent.getValue();
                out.writeChar((int) c);
                out.writeInt(charToAllBytesIdx.get(c));
                out.writeByte(bytes.length);
            }
            out.write(allBytes);
        }
        System.out.println("Finished");
    }
}

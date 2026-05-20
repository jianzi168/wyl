package formula;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PseudoCoordinateCodecTest {

    @Test
    void reorderAlignsDifferentDimOrders() {
        long[] members = PseudoCoordinateCodec.parseCommaSeparated("2001,1001,3001");
        long[] onForm = PseudoCoordinateCodec.reorder(
                members, new long[] {20L, 10L, 30L}, new long[] {10L, 20L, 30L});
        assertEquals("1001,2001,3001", PseudoCoordinateCodec.formatCommaSeparated(onForm));
    }

    @Test
    void matchesOnFormIgnoresSourceOrder() {
        assertTrue(PseudoCoordinateCodec.matchesOnForm(
                "2001,1001,3001",
                new long[] {20L, 10L, 30L},
                "1001,2001,3001",
                new long[] {10L, 20L, 30L}));
    }
}

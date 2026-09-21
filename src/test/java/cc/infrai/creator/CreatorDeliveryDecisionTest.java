package cc.infrai.creator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CreatorDeliveryDecisionTest {
    @Test
    void only_a_nonblank_generated_update_reaches_subscribers() {
        assertTrue(CreatorDeliveryService.shouldNotify("Your workbook is ready."));
        assertFalse(CreatorDeliveryService.shouldNotify(""));
        assertFalse(CreatorDeliveryService.shouldNotify(null));
    }
}

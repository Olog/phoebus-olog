/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.olog.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AttachmentTest {

    @Test
    public void testEquals() {
        Attachment attachment1 = new Attachment();
        assertFalse(attachment1.equals(null));
        assertFalse(attachment1.equals(new String("foo")));

        Attachment attachment2 = new Attachment();
        attachment1.setId("c");
        assertFalse(attachment1.equals(attachment2));

        attachment2.setId("a");

        assertFalse(attachment1.equals(attachment2));

        attachment1.setId("b");
        assertFalse(attachment1.equals(attachment2));

        attachment1.setId("a");
        assertTrue(attachment1.equals(attachment2));}
}

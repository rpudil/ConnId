/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */

package org.identityconnectors.common.event;

/**
 * NOTICE: This package is an early specification of the Events API for 1.2.x.x
 * version. Use carefully, this package may change before the final 1.2.0.0
 * release.
 * <p/>
 * A ConnectorEventHandler receives notification when a connector bundle is
 * registered or unregistered.
 *
 * @author Laszlo Hordos
 * @since 1.2
 */
public interface ConnectorEventHandler {
    /**
     * TODO: Description
     *
     * @param event
     *            The {@code ConnectorEvent} object.
     */
    public void handleEvent(ConnectorEvent event);
}

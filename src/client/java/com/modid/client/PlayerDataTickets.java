package com.modid.client;

import software.bernie.geckolib.constant.dataticket.DataTicket;

/**
 * Datos propios que capturamos del jugador en cada cuadro para que las animaciones puedan usarlos.
 */
public final class PlayerDataTickets {
    /** El jugador está tensando un arco y lo sostiene con la mano derecha. */
    public static final DataTicket<Boolean> BOW_IN_RIGHT_HAND =
            DataTicket.create("player_render_bow_right", Boolean.class);

    /** El jugador está tensando un arco y lo sostiene con la mano izquierda. */
    public static final DataTicket<Boolean> BOW_IN_LEFT_HAND =
            DataTicket.create("player_render_bow_left", Boolean.class);

    private PlayerDataTickets() {
    }
}

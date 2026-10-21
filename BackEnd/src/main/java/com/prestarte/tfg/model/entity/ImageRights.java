package com.prestarte.tfg.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Qué puede hacer la institución con la imagen de la obra mientras la
 * tiene en préstamo. Nulo o false = no autorizado: el coleccionista decide
 * explícitamente qué permite.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageRights {

    /** Reproducirla en el catálogo de la exposición. */
    @Column(name = "img_catalog")
    private Boolean catalog;

    /** Cederla a prensa para difundir la exposición. */
    @Column(name = "img_press")
    private Boolean press;

    /** Publicarla en la web y redes sociales de la institución. */
    @Column(name = "img_social")
    private Boolean socialMedia;

    /** Usarla en productos de tienda (postales, carteles...). */
    @Column(name = "img_merchandise")
    private Boolean merchandise;

    /** Permitir que el público la fotografíe en sala (sin flash). */
    @Column(name = "img_visitor_photos")
    private Boolean visitorPhotos;
}

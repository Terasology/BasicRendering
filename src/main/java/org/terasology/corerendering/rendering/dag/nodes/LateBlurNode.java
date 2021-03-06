// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.corerendering.rendering.dag.nodes;

import org.terasology.engine.config.Config;
import org.terasology.engine.config.RenderingConfig;
import org.terasology.engine.context.Context;
import org.terasology.engine.core.SimpleUri;
import org.terasology.gestalt.naming.Name;
import org.terasology.nui.properties.Range;
import org.terasology.engine.rendering.opengl.FBO;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * This class is a thin facade in front of the BlurNode class it inherits from.
 * The term "late" is due to the fact that this type of nodes is used near the
 * end of the rendering process leading to an image shown on the user display.
 *
 * Given an input FBO a blurred version of it will be stored in the given output FBO.
 * Eventually the blurred version can be used for blur-based effects such as
 * Depth of Field.
 *
 * For more information on Blur: https://en.wikipedia.org/wiki/Box_blur
 * For more information on DoF: http://en.wikipedia.org/wiki/Depth_of_field
 */
public class LateBlurNode extends BlurNode implements PropertyChangeListener {
    public static final SimpleUri FIRST_LATE_BLUR_FBO_URI = new SimpleUri("engine:fbo.firstLateBlur");
    public static final SimpleUri SECOND_LATE_BLUR_FBO_URI = new SimpleUri("engine:fbo.secondLateBlur");

    @Range(min = 0.0f, max = 16.0f)
    private static final float OVERALL_BLUR_RADIUS_FACTOR = 0.8f;

    private RenderingConfig renderingConfig;

    /**
     * Constructs a LateBlurNode instance.
     *
     * @param outputFbo The output fbo, to store the blurred image.
     */
    public LateBlurNode(String nodeUri, Name providingModule, Context context, FBO outputFbo) {
        super(nodeUri, context, providingModule, outputFbo, 0); // note: blurRadius is 0.0 at this stage.

        renderingConfig = context.get(Config.class).getRendering();
        requiresCondition(() -> renderingConfig.getBlurIntensity() != 0); // getBlurIntensity > 0 implies blur is enabled.
        renderingConfig.subscribe(RenderingConfig.BLUR_INTENSITY, this);

        updateBlurRadius(); // only here blurRadius is properly set.
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        updateBlurRadius();
        // Changing the blurRadius can potentially enable/disable the Node, meaning we have to refresh the taskList.
        super.propertyChange(event);
    }

    private void updateBlurRadius() {
        this.blurRadius = OVERALL_BLUR_RADIUS_FACTOR * Math.max(1, renderingConfig.getBlurIntensity());
    }
}

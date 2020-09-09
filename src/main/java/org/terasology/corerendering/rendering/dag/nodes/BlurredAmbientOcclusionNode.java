// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.corerendering.rendering.dag.nodes;

import org.terasology.engine.config.Config;
import org.terasology.engine.config.RenderingConfig;
import org.terasology.engine.context.Context;
import org.terasology.engine.core.SimpleUri;
import org.terasology.engine.monitoring.PerformanceMonitor;
import org.terasology.engine.rendering.assets.material.Material;
import org.terasology.engine.rendering.dag.ConditionDependentNode;
import org.terasology.engine.rendering.dag.stateChanges.BindFbo;
import org.terasology.engine.rendering.dag.stateChanges.EnableMaterial;
import org.terasology.engine.rendering.dag.stateChanges.SetInputTextureFromFbo;
import org.terasology.engine.rendering.dag.stateChanges.SetViewportToSizeOf;
import org.terasology.engine.rendering.opengl.FBO;
import org.terasology.engine.rendering.opengl.FboConfig;
import org.terasology.engine.rendering.opengl.fbms.DisplayResolutionDependentFbo;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.naming.Name;

import java.beans.PropertyChangeEvent;

import static org.terasology.engine.rendering.dag.stateChanges.SetInputTextureFromFbo.FboTexturesTypes.ColorTexture;
import static org.terasology.engine.rendering.opengl.OpenGLUtils.renderFullscreenQuad;
import static org.terasology.engine.rendering.opengl.ScalingFactors.FULL_SCALE;
import static org.terasology.engine.rendering.opengl.fbms.DisplayResolutionDependentFbo.POST_FBO_REGENERATION;

/**
 * Instances of this node work in tandem with instances of the AmbientOcclusionNode class. Together they constitute an
 * ambient occlusion pass.
 * <p>
 * This particular node blurs the ambient occlusion output produced by the AmbientOcclusionNode, making it softer and
 * more subtle. At this stage only the output of this node is used to enhance the image eventually shown on screen to
 * the user. It is currently not possible to use the (sharper) output of the AmbientOcclusionNode alone, i.e. to have
 * lower quality but faster ambient occlusion.
 * <p>
 * Ambient occlusion is a subtle visual effect that makes the rendering of the world more pleasing at the cost of some
 * additional milliseconds per frame. Disabling it may lead to increased frame rate while the gameplay remains
 * unaffected.
 * <p>
 * See http://en.wikipedia.org/wiki/Ambient_occlusion for more information on this technique.
 */
public class BlurredAmbientOcclusionNode extends ConditionDependentNode {
    public static final SimpleUri SSAO_BLURRED_FBO_URI = new SimpleUri("engine:fbo.ssaoBlurred");
    private static final ResourceUrn SSAO_BLURRED_MATERIAL_URN = new ResourceUrn("engine:prog.ssaoBlur");

    private Material ssaoBlurredMaterial;
    private float outputFboWidth;
    private float outputFboHeight;

    private FBO ssaoBlurredFbo;

    public BlurredAmbientOcclusionNode(String nodeUri, Name providingModule, Context context) {
        super(nodeUri, providingModule, context);

        RenderingConfig renderingConfig = context.get(Config.class).getRendering();
        renderingConfig.subscribe(RenderingConfig.SSAO, this);
        requiresCondition(renderingConfig::isSsao);
        addOutputFboConnection(1);
    }

    @Override
    public void setDependencies(Context context) {
        addDesiredStateChange(new EnableMaterial(SSAO_BLURRED_MATERIAL_URN));
        ssaoBlurredMaterial = getMaterial(SSAO_BLURRED_MATERIAL_URN);

        DisplayResolutionDependentFbo displayResolutionDependentFBOs = context.get(DisplayResolutionDependentFbo.class);

        ssaoBlurredFbo = requiresFbo(new FboConfig(SSAO_BLURRED_FBO_URI, FULL_SCALE, FBO.Type.DEFAULT),
                displayResolutionDependentFBOs);
        addOutputFboConnection(1, ssaoBlurredFbo);
        addDesiredStateChange(new BindFbo(ssaoBlurredFbo));
        addDesiredStateChange(new SetViewportToSizeOf(ssaoBlurredFbo));
        displayResolutionDependentFBOs.subscribe(POST_FBO_REGENERATION, this);

        retrieveFboDimensions();

        // DisplayResolutionDependentFbo displayResolutionDependentFBOs = context.get(DisplayResolutionDependentFbo
        // .class);
        addDesiredStateChange(new SetInputTextureFromFbo(0, this.getInputFboData(1), ColorTexture,
                displayResolutionDependentFBOs, SSAO_BLURRED_MATERIAL_URN, "tex"));
    }

    /**
     * If Ambient Occlusion is enabled in the render settings, this method generates and stores the necessary images
     * into their own FBOs. The stored images are eventually combined with others to enhance the image shown on screen.
     * <p>
     * For further information on Ambient Occlusion see: http://en.wikipedia.org/wiki/Ambient_occlusion
     */
    @Override
    public void process() {
        PerformanceMonitor.startActivity("rendering/" + getUri());

        ssaoBlurredMaterial.setFloat2("texelSize", 1.0f / outputFboWidth, 1.0f / outputFboHeight, true);

        renderFullscreenQuad();

        PerformanceMonitor.endActivity();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();

        switch (propertyName) {
            case RenderingConfig.SSAO:
                super.propertyChange(event);
                break;

            case POST_FBO_REGENERATION:
                retrieveFboDimensions();
                break;

            // default: no other cases are possible - see subscribe operations in initialize.
        }
    }

    private void retrieveFboDimensions() {
        outputFboWidth = ssaoBlurredFbo.width();
        outputFboHeight = ssaoBlurredFbo.height();
    }
}

// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.corerendering.rendering.dag.nodes;

import org.terasology.engine.context.Context;
import org.terasology.engine.monitoring.PerformanceMonitor;
import org.terasology.engine.rendering.assets.material.Material;
import org.terasology.engine.rendering.assets.mesh.Mesh;
import org.terasology.engine.rendering.dag.ConditionDependentNode;
import org.terasology.engine.rendering.dag.stateChanges.BindFbo;
import org.terasology.engine.rendering.dag.stateChanges.EnableMaterial;
import org.terasology.engine.rendering.dag.stateChanges.SetInputTextureFromFbo;
import org.terasology.engine.rendering.dag.stateChanges.SetViewportToSizeOf;
import org.terasology.engine.rendering.opengl.BaseFboManager;
import org.terasology.engine.rendering.opengl.FBO;
import org.terasology.engine.rendering.opengl.FboConfig;
import org.terasology.engine.utilities.Assets;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.naming.Name;

import static org.terasology.engine.rendering.dag.stateChanges.SetInputTextureFromFbo.FboTexturesTypes.ColorTexture;

/**
 * Instances of this class take the content of the color attachment of an input FBO
 * and downsamples it into the color attachment of a smaller output FBO.
 */
public class DownSamplerNode extends ConditionDependentNode {
    private static final String TEXTURE_NAME = "tex";
    private static final ResourceUrn DOWN_SAMPLER_MATERIAL_URN = new ResourceUrn("CoreRendering:downSampler");

    private FBO outputFbo;
    private Material downSampler;
    private BaseFboManager inputFboManager;
    private Mesh renderQuad;

    /**
     * Constructs the DownSamplerNode instance.
     *
     * @param inputFboManager the FBO manager from which to retrieve the input FBO
     * @param outputFboConfig an FboConfig instance describing the output FBO, to be retrieved from the FBO manager
     * @param outputFboManager the FBO manager from which to retrieve the output FBO
     */
    public DownSamplerNode(String nodeUri, Context context, Name providingModule,
                           BaseFboManager inputFboManager,
                           FboConfig outputFboConfig, BaseFboManager outputFboManager) {
        super(nodeUri, providingModule, context);

        // OUT
        // TODO get rid of this? why load input fbo from dependency when I still need this
        this.inputFboManager = inputFboManager;
        addOutputFboConnection(1);
        outputFbo = requiresFbo(outputFboConfig, outputFboManager);


        this.renderQuad = Assets.get(new ResourceUrn("engine:ScreenQuad"), Mesh.class)
                .orElseThrow(() -> new RuntimeException("Failed to resolve render Quad"));
    }

    /**
     * Processes the input FBO downsampling its color attachment into the color attachment of the output FBO.
     */
    @Override
    public void process() {
        PerformanceMonitor.startActivity("rendering/" + getUri());

        downSampler.setFloat("size", outputFbo.width(), true);

        renderQuad.render();

        PerformanceMonitor.endActivity();
    }

    @Override
    public void setDependencies(Context context) {
        addOutputFboConnection(1, outputFbo);
        addDesiredStateChange(new BindFbo(outputFbo));
        addDesiredStateChange(new SetViewportToSizeOf(outputFbo));
        addDesiredStateChange(new SetInputTextureFromFbo(0, this.getInputFboData(1), ColorTexture, inputFboManager,
                DOWN_SAMPLER_MATERIAL_URN, TEXTURE_NAME));

        addDesiredStateChange(new EnableMaterial(DOWN_SAMPLER_MATERIAL_URN));
        downSampler = getMaterial(DOWN_SAMPLER_MATERIAL_URN);
    }
}

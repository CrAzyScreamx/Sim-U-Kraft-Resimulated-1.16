package com.resimulators.simukraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.resimulators.simukraft.Reference;
import com.resimulators.simukraft.common.jobs.Profession;
import com.resimulators.simukraft.common.world.Structure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;

public class GuiBuilder extends GuiBaseJob {
    private Button Build;
    private Button CustomBack;

    private boolean loaded = false;
    private ArrayList<Structure> structures;

    public GuiBuilder(ITextComponent component, ArrayList<Integer> ids, BlockPos pos, @Nullable int id) {
        super(component, ids, pos, id, Profession.BUILDER.getId());
        loaded = true;// need to test
    }


    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
        if (loaded) {
            addButton(Build = new LargeButton(width / 2 - 55, height - 55, 110, 42, new StringTextComponent("Build"), (Build -> {
                super.hideAll();
                CustomBack.visible = true;
                state = State.SELECTBULDING;
            })));
            //Build.active=false;
            addButton(CustomBack = new Button(width - 120, height - 30, 110, 20, new StringTextComponent("Back"), (Back -> {
                super.Back.onPress();
                if (state == State.SELECTBULDING) {
                    state = State.MAIN;
                    showMainMenu();

                }
                if (state == State.BUILDINGINFO) {
                    state = State.SELECTBULDING;
                }

            }


            )));
            if (!isHired()) {
                Build.active = false;
            }
            CustomBack.visible = false;
        }
    }

    public void loadBuildings(ArrayList<Structure> structures) {
        this.loaded = true;
        this.structures = structures;
        init(); //init
    }

    public void setStructures(ArrayList<Structure> structures) {
        this.structures = structures;

    }

    @Override
    public void render(MatrixStack stack, int p_render_1_, int p_render_2_, float p_render_3_) {
        renderBackground(stack);
        if (loaded) super.render(stack, p_render_1_, p_render_2_, p_render_3_);
        else {
            font.drawString(stack, "Loading", (float) width / 2 - font.getStringWidth("Loading") / 2, (float) height / 2, Color.WHITE.getRGB());
        }

    }

    @Override
    public void showMainMenu() {
        super.showMainMenu();
        Build.visible = true;
    }

    static class State extends GuiBaseJob.State {
        private static final int SELECTBULDING = nextID();
        private static final int BUILDINGINFO = nextID();


    }

    private class LargeButton extends Button {
        final ResourceLocation LARGE_BUTTON = new ResourceLocation(Reference.MODID, "textures/gui/large_button.png");

        public LargeButton(int widthIn, int heightIn, int width, int height, ITextComponent text, IPressable onPress) {
            super(widthIn, heightIn, width, height, text, onPress);
        }

        @Override
        public void renderButton(MatrixStack stack, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
            Minecraft minecraft = Minecraft.getInstance();
            FontRenderer fontrenderer = minecraft.fontRenderer;
            minecraft.getTextureManager().bindTexture(LARGE_BUTTON);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
            int i = this.getYImage(this.isHovered);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            this.blit(stack, this.x, this.y, 0, 1 + i * 42, this.width / 2, this.height);
            this.blit(stack, this.x + this.width / 2, this.y, 200 - this.width / 2, 1 + i * 42, this.width / 2, this.height);
            this.renderBg(stack, minecraft, p_renderButton_1_, p_renderButton_2_);
            int j = getFGColor();
            this.drawCenteredString(stack, fontrenderer, this.getMessage().getString(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0F) << 24);

        }


        @Override
        public int getYImage(boolean hovered) {
            int i = 0;
            if (!this.active) {
                i = 1;
            } else if (hovered) {
                i = 2;
            }

            return i;
        }
    }
}

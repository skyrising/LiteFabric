package de.skyrising.litefabric.liteloader.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GL {
    public static final VertexFormat VF_BLOCK = VertexFormats.POSITION_COLOR_TEXTURE;
    public static final VertexFormat VF_ITEM = VertexFormats.POSITION_COLOR_TEXTURE_NORMAL;
    public static final VertexFormat VF_OLDMODEL_POSITION_TEX_NORMAL = VertexFormats.POSITION_TEXTURE_NORMAL;
    public static final VertexFormat VF_PARTICLE_POSITION_TEX_COLOR_LMAP = VertexFormats.POSITION_TEXTURE_COLOR_SF;
    public static final VertexFormat VF_POSITION = VertexFormats.POSITION;
    public static final VertexFormat VF_POSITION_COLOR = VertexFormats.POSITION_COLOR;
    public static final VertexFormat VF_POSITION_TEX = VertexFormats.POSITION_TEXTURE_F;
    public static final VertexFormat VF_POSITION_NORMAL = VertexFormats.POSITION_NORMAL;
    public static final VertexFormat VF_POSITION_TEX_COLOR = VertexFormats.POSITION_COLOR_TEXTURE_F;
    public static final VertexFormat VF_POSITION_TEX_NORMAL = VertexFormats.POSITION_TEXTURE_F_NORMAL;
    public static final VertexFormat VF_POSITION_TEX_LMAP_COLOR = VertexFormats.POSITION_TEXTURE_FS;
    public static final VertexFormat VF_POSITION_TEX_COLOR_NORMAL = VertexFormats.POSITION_TEXTURE_COLOR_NORMAL;

    public static void glPushAttrib() {
        // GL_ENABLE_BIT | GL_LIGHTING_BIT
        GlStateManager.method_30264();
    }

    public static void glPushAttrib(int mask) {
        GL11.glPushAttrib(mask);
    }

    public static void glPopAttrib() {
        GlStateManager.popAttributes();
    }

    public static void glDisableAlphaTest() {
        GlStateManager.disableAlphaTest();
    }

    public static void glEnableAlphaTest() {
        GlStateManager.enableAlphaTest();
    }

    public static void glAlphaFunc(int func, float ref) {
        GlStateManager.alphaFunc(func, ref);
    }

    public static void glEnableLighting() {
        GlStateManager.enableLighting();
    }

    public static void glDisableLighting() {
        GlStateManager.disableLighting();
    }

    public static void glEnableLight(int light) {
        GlStateManager.enableLight(light);
    }

    public static void glDisableLight(int light) {
        GlStateManager.disableLight(light);
    }

    public static void glLight(int light, int pname, FloatBuffer params) {
        GlStateManager.light(light, pname, params);
    }

    public static void glLightModel(int pname, FloatBuffer params) {
        GlStateManager.lightModel(pname, params);
    }

    public static void glLightModeli(int pname, int param) {
        GL11.glLightModeli(pname, param);
    }

    public static void glEnableColorMaterial() {
        GlStateManager.enableColorMaterial();
    }

    public static void glDisableColorMaterial() {
        GlStateManager.disableColorMaterial();
    }

    public static void glColorMaterial(int face, int mode) {
        GlStateManager.colorMaterial(face, mode);
    }

    public static void glDisableDepthTest() {
        GlStateManager.disableDepthTest();
    }

    public static void glEnableDepthTest() {
        GlStateManager.enableDepthTest();
    }

    public static void glDepthFunc(int func) {
        GlStateManager.depthFunc(func);
    }

    public static void glDepthMask(boolean flag) {
        GlStateManager.depthMask(flag);
    }

    public static void glDisableBlend() {
        GlStateManager.disableBlend();
    }

    public static void glEnableBlend() {
        GlStateManager.enableBlend();
    }

    public static void glBlendFunc(int sfactor, int dfactor) {
        GlStateManager.blendFunc(sfactor, dfactor);
    }

    public static void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        GlStateManager.blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    public static void glEnableFog() {
        GlStateManager.enableFog();
    }

    public static void glDisableFog() {
        GlStateManager.disableFog();
    }

    public static void glSetFogMode(GlStateManager.FogMode mode) {
        GlStateManager.method_30288(mode);
    }

    public static void glSetFogDensity(float density) {
        GlStateManager.fogDensity(density);
    }

    public static void glSetFogStart(float start) {
        GlStateManager.fogStart(start);
    }

    public static void glSetFogEnd(float end) {
        GlStateManager.fogEnd(end);
    }

    public static void glSetFogColour(FloatBuffer colour) {
        GlStateManager.fog(GL11.GL_FOG_COLOR, colour);
    }

    public static void glFogi(int pname, int param) {
        GlStateManager.fogi(pname, param);
    }

    public static void glFogf(int pname, float param) {
        GL11.glFogf(pname, param);
    }

    public static void glEnableCulling() {
        GlStateManager.enableCull();
    }

    public static void glDisableCulling() {
        GlStateManager.disableCull();
    }

    public static void glCullFace(GlStateManager.CullFace mode) {
        GlStateManager.cullFace(mode);
    }

    public static void glEnablePolygonOffset() {
        GlStateManager.enablePolygonOffset();
    }

    public static void glDisablePolygonOffset() {
        GlStateManager.disableLineOffset();
    }

    public static void glPolygonOffset(float factor, float units) {
        GlStateManager.polygonOffset(factor, units);
    }

    public static void glEnableColorLogic() {
        GlStateManager.enableColorLogicOp();
    }

    public static void glDisableColorLogic() {
        GlStateManager.disableColorLogicOp();
    }

    public static void glLogicOp(int opcode) {
        GlStateManager.logicOp(opcode);
    }

    public static void glEnableTexGenCoord(GlStateManager.TexCoord tex) {
        GlStateManager.enableTexGen(tex);
    }

    public static void glDisableTexGenCoord(GlStateManager.TexCoord tex) {
        GlStateManager.disableTexGen(tex);
    }

    public static void glTexGeni(GlStateManager.TexCoord tex, int mode) {
        GlStateManager.texGenMode(tex, mode);
    }

    public static void glTexGen(GlStateManager.TexCoord tex, int pname, FloatBuffer params) {
        GlStateManager.texGenParam(tex, pname, params);
    }

    public static void glSetActiveTextureUnit(int texture) {
        GlStateManager.activeTexture(texture);
    }

    public static void glEnableTexture2D() {
        GlStateManager.enableTexture();
    }

    public static void glDisableTexture2D() {
        GlStateManager.disableTexture();
    }

    public static int glGenTextures() {
        return GlStateManager.genTextures();
    }

    public static void glDeleteTextures(int textureName) {
        GlStateManager.deleteTexture(textureName);
    }

    public static void glBindTexture2D(int textureName) {
        GlStateManager.bindTexture(textureName);
    }

    public static void glEnableNormalize() {
        GlStateManager.enableRescaleNormal();
    }

    public static void glDisableNormalize() {
        GlStateManager.disableRescaleNormal();
    }

    public static void glShadeModel(int mode) {
        GlStateManager.shadeModel(mode);
    }

    public static void glEnableRescaleNormal() {
        GlStateManager.enableRescaleNormal();
    }

    public static void glDisableRescaleNormal() {
        GlStateManager.disableRescaleNormal();
    }

    public static void glViewport(int x, int y, int width, int height) {
        GlStateManager.viewport(x, y, width, height);
    }

    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GlStateManager.colorMask(red, green, blue, alpha);
    }

    public static void glClearDepth(double depth) {
        GlStateManager.clearDepth(depth);
    }

    public static void glClearColor(float red, float green, float blue, float alpha) {
        GlStateManager.clearColor(red, green, blue, alpha);
    }

    public static void glClear(int mask) {
        GlStateManager.clear(mask);
    }

    public static void glMatrixMode(int mode) {
        GlStateManager.matrixMode(mode);
    }

    public static void glLoadIdentity() {
        GlStateManager.loadIdentity();
    }

    public static void glPushMatrix() {
        GlStateManager.pushMatrix();
    }

    public static void glPopMatrix() {
        GlStateManager.popMatrix();
    }

    public static void glGetFloat(int pname, FloatBuffer params) {
        GlStateManager.getFloat(pname, params);
    }

    public static float glGetFloat(int pname) {
        return GL11.glGetFloat(pname);
    }

    public static void glGetDouble(int pname, DoubleBuffer params) {
        GL11.glGetDouble(pname, params);
    }

    public static double glGetDouble(int pname) {
        return GL11.glGetDouble(pname);
    }

    public static void glGetInteger(int pname, IntBuffer params) {
        GL11.glGetInteger(pname, params);
    }

    public static int glGetInteger(int pname) {
        return GL11.glGetInteger(pname);
    }

    public static void glGetBoolean(int pname, ByteBuffer params) {
        GL11.glGetBoolean(pname, params);
    }

    public static boolean glGetBoolean(int pname) {
        return GL11.glGetBoolean(pname);
    }

    public static void gluProject(float objx, float objy, float objz, FloatBuffer modelMatrix, FloatBuffer projMatrix, IntBuffer viewport,
                                  FloatBuffer winPos) {
        GLU.gluProject(objx, objy, objz, modelMatrix, projMatrix, viewport, winPos);
    }

    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        GLU.gluPerspective(fovy, aspect, zNear, zFar);
    }

    public static void glOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {
        GlStateManager.ortho(left, right, bottom, top, zNear, zFar);
    }

    public static void glRotatef(float angle, float x, float y, float z) {
        GlStateManager.rotatef(angle, x, y, z);
    }

    public static void glRotated(double angle, double x, double y, double z) {
        GL11.glRotated(angle, x, y, z);
    }

    public static void glScalef(float x, float y, float z) {
        GlStateManager.scalef(x, y, z);
    }

    public static void glScaled(double x, double y, double z) {
        GlStateManager.scaled(x, y, z);
    }

    public static void glTranslatef(float x, float y, float z) {
        GlStateManager.translatef(x, y, z);
    }

    public static void glTranslated(double x, double y, double z) {
        GlStateManager.translated(x, y, z);
    }

    public static void glMultMatrix(FloatBuffer m) {
        GlStateManager.multMatrix(m);
    }

    public static void glColor4f(float red, float green, float blue, float alpha) {
        GlStateManager.color4f(red, green, blue, alpha);
    }

    public static void glColor3f(float red, float green, float blue) {
        GlStateManager.color3f(red, green, blue);
    }

    public static void glResetColor() {
        GlStateManager.clearCurrentColor();
    }

    public static void glEnableClientState(int cap) {
        GlStateManager.enableClientState(cap);
    }

    public static void glDisableClientState(int cap) {
        GlStateManager.disableClientState(cap);
    }

    public static void glDrawArrays(int mode, int first, int count) {
        GlStateManager.drawArrays(mode, first, count);
    }

    public static void glCallList(int list) {
        GlStateManager.callList(list);
    }

    public static void glCallLists(IntBuffer lists) {
        GL11.glCallLists(lists);
    }

    public static int glGenLists(int range) {
        return GlStateManager.genLists(range);
    }

    public static void glNewList(int list, int mode) {
        GlStateManager.newList(list, mode);
    }

    public static void glEndList() {
        GlStateManager.endList();
    }

    public static void glDeleteLists(int list, int range) {
        GlStateManager.deleteLists(list, range);
    }

    public static void glLineWidth(float width) {
        GlStateManager.lineWidth(width);
    }

    public static void glPolygonMode(int face, int mode) {
        GlStateManager.polygonMode(face, mode);
    }

    public static void glPixelStorei(int pname, int param) {
        GlStateManager.pixelStore(pname, param);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        GL11.glReadPixels(x, y, width, height, format, type, pixels);
    }

    public static void glReadPixels(int x, int y, int width, int height, int format, int type, IntBuffer pixels) {
        GlStateManager.readPixels(x, y, width, height, format, type, pixels);
    }

    public static void glGetTexImage(int target, int level, int format, int type, ByteBuffer pixels) {
        GL11.glGetTexImage(target, level, format, type, pixels);
    }

    public static void glGetTexImage(int target, int level, int format, int type, IntBuffer pixels) {
        GlStateManager.getTexImage(target, level, format, type, pixels);
    }

    public static void glNormalPointer(int stride, FloatBuffer pointer) {
        GL11.glNormalPointer(stride, pointer);
    }

    public static void glNormalPointer(int type, int stride, ByteBuffer pointer) {
        GlStateManager.normalPointer(type, stride, pointer);
    }

    public static void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {
        GL11.glTexCoordPointer(size, stride, pointer);
    }

    public static void glTexCoordPointer(int size, int type, int stride, int pointerBufferOffset) {
        GlStateManager.texCoordPointer(size, type, stride, pointerBufferOffset);
    }

    public static void glTexCoordPointer(int size, int type, int stride, ByteBuffer pointer) {
        GlStateManager.texCoordPointer(size, type, stride, pointer);
    }

    public static void glVertexPointer(int size, int stride, FloatBuffer pointer) {
        GL11.glVertexPointer(size, stride, pointer);
    }

    public static void glVertexPointer(int size, int type, int stride, ByteBuffer pointer) {
        GlStateManager.vertexPointer(size, type, stride, pointer);
    }

    public static void glEdgeFlagPointer(int stride, ByteBuffer pointer) {
        GL11.glEdgeFlagPointer(stride, pointer);
    }
}
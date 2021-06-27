/*
 * Code by Martin Sojka
 * Link: https://stackoverflow.com/questions/16089304/javafx-imageview-without-any-smoothing/43192263
 */

package view;

import com.sun.javafx.sg.prism.NGImageView;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.prism.Graphics;
import com.sun.prism.Texture;
import com.sun.prism.impl.BaseResourceFactory;
import com.sun.prism.Image;
import javafx.scene.image.ImageView;

@SuppressWarnings("restriction")
public
class PixelatedImageView extends ImageView {

    @Override
    protected NGNode impl_createPeer() {
        return new NGImageView() {
            private Image image;

            @Override
            public void setImage(Object img) {
                super.setImage(img);
                image = (Image) img;
            }

            @Override
            protected void renderContent(Graphics g) {
                BaseResourceFactory factory = (BaseResourceFactory) g.getResourceFactory();
                Texture tex = factory.getCachedTexture(image, Texture.WrapMode.CLAMP_TO_EDGE);
                tex.setLinearFiltering(false);
                tex.unlock();
                super.renderContent(g);
            }
        };
    }
}
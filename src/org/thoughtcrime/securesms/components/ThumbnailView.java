package org.thoughtcrime.securesms.components;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.database.PartDatabase;
import org.thoughtcrime.securesms.mms.DecryptableStreamUriLoader.DecryptableUri;
import org.thoughtcrime.securesms.mms.RoundedCorners;
import org.thoughtcrime.securesms.mms.Slide;
import org.thoughtcrime.securesms.mms.SlideDeck;
import org.thoughtcrime.securesms.util.FutureTaskListener;
import org.thoughtcrime.securesms.util.ListenableFutureTask;
import org.thoughtcrime.securesms.util.Util;
import org.thoughtcrime.securesms.util.ViewUtil;

import ws.com.google.android.mms.pdu.PduPart;

public class ThumbnailView extends FrameLayout {
  private static final String TAG = ThumbnailView.class.getSimpleName();

  private boolean             hideControls;
  private ImageView           image;
  private ImageView           removeButton;
  private TransferControlView transferControls;
  private int                 backgroundColorHint;
  private int                 radius;

  private ListenableFutureTask<SlideDeck> slideDeckFuture        = null;
  private SlideDeckListener               slideDeckListener      = null;
  private ThumbnailClickListener          thumbnailClickListener = null;
  private ThumbnailClickListener          downloadClickListener  = null;
  private String                          slideId                = null;
  private Slide                           slide                  = null;

  public ThumbnailView(Context context) {
    this(context, null);
  }

  public ThumbnailView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ThumbnailView(final Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    inflate(context, R.layout.thumbnail_view, this);
    radius = getResources().getDimensionPixelSize(R.dimen.message_bubble_corner_radius);
    image  = (ImageView) findViewById(R.id.thumbnail_image);

    if (attrs != null) {
      TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ThumbnailView, 0, 0);
      backgroundColorHint = typedArray.getColor(0, Color.BLACK);
      typedArray.recycle();
    }
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (removeButton != null) {
      final int paddingHorizontal = removeButton.getWidth()  / 2;
      final int paddingVertical   = removeButton.getHeight() / 2;
      image.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, 0);
    }
  }

  private ImageView getRemoveButton() {
    if (removeButton == null) removeButton = ViewUtil.inflateStub(this, R.id.remove_button_stub);
    return removeButton;
  }

  private TransferControlView getTransferControls() {
    if (transferControls == null) transferControls = ViewUtil.inflateStub(this, R.id.transfer_controls_stub);

    return transferControls;
  }

  public void setBackgroundColorHint(int color) {
    this.backgroundColorHint = color;
  }

  public void setImageResource(@Nullable MasterSecret masterSecret,
                               long id, long timestamp,
                               @NonNull ListenableFutureTask<SlideDeck> slideDeckFuture)
  {
    if (this.slideDeckFuture != null && this.slideDeckListener != null) {
      this.slideDeckFuture.removeListener(this.slideDeckListener);
    }

    String slideId = id + "::" + timestamp;

    if (!slideId.equals(this.slideId)) {
      if (transferControls != null) transferControls.clear();
      image.setImageDrawable(null);
      this.slide   = null;
      this.slideId = slideId;
    }

    this.slideDeckListener = new SlideDeckListener(masterSecret);
    this.slideDeckFuture   = slideDeckFuture;
    this.slideDeckFuture.addListener(this.slideDeckListener);
  }

  public void setImageResource(@NonNull Slide slide, @Nullable MasterSecret masterSecret) {
    if (Util.equals(slide, this.slide)) {
      Log.w(TAG, "Not re-loading slide " + slide.getPart().getPartId());
      return;
    }
    if (!isContextValid()) {
      Log.w(TAG, "Not loading slide, context is invalid");
      return;
    }

    Log.w(TAG, "loading part with id " + slide.getPart().getPartId() + ", progress " + slide.getTransferProgress());

    this.slide = slide;
    buildGlideRequest(slide, masterSecret).into(image);

    if (this.slide.getTransferProgress() == PartDatabase.TRANSFER_PROGRESS_DONE) {
      setOnClickListener(new ThumbnailClickDispatcher(thumbnailClickListener, slide));
    } else {
      setOnClickListener(null);
    }

    if (!hideControls) {
      getTransferControls().setSlide(slide);
      getTransferControls().setDownloadClickListener(new ThumbnailClickDispatcher(downloadClickListener, slide));
      getTransferControls().setVisibility(View.VISIBLE);
    }
  }

  public void setThumbnailClickListener(ThumbnailClickListener listener) {
    this.thumbnailClickListener = listener;
  }

  public void setRemoveClickListener(OnClickListener listener) {
    getRemoveButton().setOnClickListener(listener);
    final int pad = getResources().getDimensionPixelSize(R.dimen.media_bubble_remove_button_size);
    image.setPadding(pad, pad, pad, 0);
  }

  public void setDownloadClickListener(ThumbnailClickListener listener) {
    this.downloadClickListener = listener;
  }

  public void clear() {
    if (isContextValid())         Glide.clear(image);
    if (slideDeckFuture != null)  slideDeckFuture.removeListener(slideDeckListener);
    if (transferControls != null) transferControls.clear();
    slide             = null;
    slideId           = null;
    slideDeckFuture   = null;
    slideDeckListener = null;
  }

  public void hideControls(boolean hideControls) {
    this.hideControls = hideControls;
    if (hideControls && transferControls != null) transferControls.setVisibility(View.GONE);
  }

  public void showProgressSpinner() {
    getTransferControls().showProgressSpinner();
  }

  @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
  private boolean isContextValid() {
    return !(getContext() instanceof Activity)            ||
           VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR1 ||
           !((Activity)getContext()).isDestroyed();
  }

  private GenericRequestBuilder buildGlideRequest(@NonNull Slide slide,
                                                  @Nullable MasterSecret masterSecret)
  {
    final GenericRequestBuilder builder;
    if (slide.getThumbnailUri() != null) {
      builder = buildThumbnailGlideRequest(slide, masterSecret);
    } else {
      builder = buildPlaceholderGlideRequest(slide);
    }

    if (slide.isInProgress() && !hideControls) {
      return builder;
    } else {
      return builder.error(R.drawable.ic_missing_thumbnail_picture);
    }
  }

  private GenericRequestBuilder buildThumbnailGlideRequest(Slide slide, MasterSecret masterSecret) {

    final GenericRequestBuilder builder;
    if   (slide.isDraft()) builder = buildDraftGlideRequest(slide, masterSecret);
    else                   builder = buildPartGlideRequest(slide, masterSecret);
    return builder;
  }

  private GenericRequestBuilder buildDraftGlideRequest(Slide slide, MasterSecret masterSecret) {
    final DrawableTypeRequest<?> request;
    if (masterSecret == null) request = Glide.with(getContext()).load(slide.getThumbnailUri());
    else                      request = Glide.with(getContext()).load(new DecryptableUri(masterSecret, slide.getThumbnailUri()));

    return request.transform(new RoundedCorners(getContext(), false, radius, backgroundColorHint))
                  .listener(new PduThumbnailSetListener(slide.getPart()));
  }

  private GenericRequestBuilder buildPartGlideRequest(Slide slide, MasterSecret masterSecret) {
    if (masterSecret == null) {
      throw new IllegalStateException("null MasterSecret when loading non-draft thumbnail");
    }

    return Glide.with(getContext()).load(new DecryptableUri(masterSecret, slide.getThumbnailUri()))
                                   .crossFade()
                                   .transform(new RoundedCorners(getContext(), true, radius, backgroundColorHint));
  }

  private GenericRequestBuilder buildPlaceholderGlideRequest(Slide slide) {
    return Glide.with(getContext()).load(slide.getPlaceholderRes(getContext().getTheme()))
                                   .asBitmap()
                                   .fitCenter();
  }

  private class SlideDeckListener implements FutureTaskListener<SlideDeck> {
    private final MasterSecret masterSecret;

    public SlideDeckListener(MasterSecret masterSecret) {
      this.masterSecret = masterSecret;
    }

    @Override
    public void onSuccess(final SlideDeck slideDeck) {
      if (slideDeck == null) return;

      final Slide slide = slideDeck.getThumbnailSlide();

      if (slide != null) {
        Util.runOnMain(new Runnable() {
          @Override
          public void run() {
            setImageResource(slide, masterSecret);
          }
        });
      } else {
        Util.runOnMain(new Runnable() {
          @Override
          public void run() {
            Log.w(TAG, "Resolved slide was null!");
            setVisibility(View.GONE);
          }
        });
      }
    }

    @Override
    public void onFailure(Throwable error) {
      Log.w(TAG, error);
      Util.runOnMain(new Runnable() {
        @Override
        public void run() {
          Log.w(TAG, "onFailure!");
          setVisibility(View.GONE);
        }
      });
    }
  }

  public interface ThumbnailClickListener {
    void onClick(View v, Slide slide);
  }

  private static class ThumbnailClickDispatcher implements View.OnClickListener {
    private ThumbnailClickListener listener;
    private Slide                  slide;

    public ThumbnailClickDispatcher(ThumbnailClickListener listener, Slide slide) {
      this.listener = listener;
      this.slide    = slide;
    }

    @Override
    public void onClick(View view) {
      if (listener != null)
      {
        listener.onClick(view, slide);
      }
    }
  }

  private class PduThumbnailSetListener implements RequestListener<Object, GlideDrawable> {
    private PduPart part;

    public PduThumbnailSetListener(@NonNull PduPart part) {
      this.part = part;
    }

    @Override
    public boolean onException(Exception e, Object model, Target<GlideDrawable> target, boolean isFirstResource) {
      return false;
    }

    @Override
    public boolean onResourceReady(GlideDrawable resource, Object model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
      if (resource instanceof GlideBitmapDrawable) {
        Log.w(TAG, "onResourceReady() for a Bitmap. Saving.");
        part.setThumbnail(((GlideBitmapDrawable)resource).getBitmap());
      }
      LayoutParams layoutParams = (LayoutParams) getRemoveButton().getLayoutParams();
      if (resource.getIntrinsicWidth() < getWidth()) {
        layoutParams.topMargin   = 0;
        layoutParams.rightMargin = Math.max(0, (getWidth() - image.getPaddingRight() - resource.getIntrinsicWidth()) / 2);
      } else {
        layoutParams.topMargin   = Math.max(0, (getHeight() - image.getPaddingTop() - resource.getIntrinsicHeight()) / 2);
        layoutParams.rightMargin = 0;
      }
      getRemoveButton().setLayoutParams(layoutParams);
      return false;
    }
  }
}

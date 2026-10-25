import os
import shutil
from PIL import Image, ImageDraw

uploaded_path = r'C:/Users/gokul/.gemini/antigravity/brain/0dd09fe2-8e54-40ea-9a62-3f7ef97e952c/.user_uploaded/media_1792934042705_023d1eb8.jpg'
src_img = Image.open(uploaded_path).convert('RGB')
w, h = src_img.size

print(f"Loaded master source image: {uploaded_path} ({w}x{h})")

def create_rounded_icon(src, size, radius_pct=0.18):
    resized = src.resize((size, size), Image.Resampling.LANCZOS).convert('RGBA')
    scale = 4
    mask_size = size * scale
    mask = Image.new('L', (mask_size, mask_size), 0)
    draw = ImageDraw.Draw(mask)
    r = int(mask_size * radius_pct)
    draw.rounded_rectangle([0, 0, mask_size - 1, mask_size - 1], radius=r, fill=255)
    mask = mask.resize((size, size), Image.Resampling.LANCZOS)
    output = resized.copy()
    output.putalpha(mask)
    return output

# 1. Generate 1024x1024 master rounded PNG
logo_1024_rounded = create_rounded_icon(src_img, 1024, radius_pct=0.18)

# 2. Generate all Windows WPF Assets
wpf_assets_dir = r'C:/KnowToMigrate/apps/windows-wpf/Assets'
os.makedirs(wpf_assets_dir, exist_ok=True)

# Save main logo.png (1024x1024 rounded)
wpf_logo_png = os.path.join(wpf_assets_dir, 'logo.png')
logo_1024_rounded.save(wpf_logo_png, 'PNG')
print(f"Saved: {wpf_logo_png}")

# Save logo.jpg
wpf_logo_jpg = os.path.join(wpf_assets_dir, 'logo.jpg')
src_img.save(wpf_logo_jpg, 'JPEG', quality=95)
print(f"Saved: {wpf_logo_jpg}")

# Save sized PNGs (16, 24, 32, 48, 64, 128, 256)
icon_sizes = [16, 24, 32, 48, 64, 128, 256]
sized_images = {}
for s in icon_sizes:
    img = create_rounded_icon(src_img, s, radius_pct=0.18)
    sized_images[s] = img
    out_path = os.path.join(wpf_assets_dir, f'logo_{s}.png')
    img.save(out_path, 'PNG')
    print(f"Saved: {out_path}")

# Build multi-resolution KnowToMigrate.ico
ico_path = os.path.join(wpf_assets_dir, 'KnowToMigrate.ico')
largest = sized_images[256]
append_list = [sized_images[s] for s in [16, 24, 32, 48, 64, 128]]
largest.save(ico_path, format='ICO', sizes=[(s, s) for s in icon_sizes], append_images=append_list)
print(f"Saved: {ico_path}")

# 3. Update master_logo in design/
design_dir = r'C:/KnowToMigrate/design'
os.makedirs(design_dir, exist_ok=True)
src_img.save(os.path.join(design_dir, 'master_logo.jpg'), 'JPEG', quality=95)
logo_1024_rounded.save(os.path.join(design_dir, 'master_logo_rounded.png'), 'PNG')
print("Updated design/master_logo.jpg and master_logo_rounded.png")

# 4. Update website assets
website_public_dir = r'C:/KnowToMigrate/apps/website/public'
if os.path.exists(website_public_dir):
    shutil.copyfile(wpf_logo_png, os.path.join(website_public_dir, 'logo.png'))
    shutil.copyfile(wpf_logo_jpg, os.path.join(website_public_dir, 'logo.jpg'))
    shutil.copyfile(ico_path, os.path.join(website_public_dir, 'favicon.ico'))
    sized_images[32].save(os.path.join(website_public_dir, 'favicon.png'), 'PNG')
    create_rounded_icon(src_img, 192, radius_pct=0.18).save(os.path.join(website_public_dir, 'logo192.png'), 'PNG')
    create_rounded_icon(src_img, 512, radius_pct=0.18).save(os.path.join(website_public_dir, 'logo512.png'), 'PNG')
    print("Updated website public assets")

# 5. Update Android drawables
android_drawable_dir = r'C:/KnowToMigrate/apps/android/app/src/main/res/drawable'
if os.path.exists(android_drawable_dir):
    create_rounded_icon(src_img, 512, radius_pct=0.18).save(os.path.join(android_drawable_dir, 'logo.png'), 'PNG')
    create_rounded_icon(src_img, 512, radius_pct=0.18).save(os.path.join(android_drawable_dir, 'splash_logo.png'), 'PNG')
    print("Updated Android drawables")

# 6. Update releases directory if present
releases_assets_dir = r'C:/KnowToMigrate/releases/windows/publish/Assets'
if os.path.exists(releases_assets_dir):
    shutil.copyfile(wpf_logo_png, os.path.join(releases_assets_dir, 'logo.png'))
    shutil.copyfile(wpf_logo_jpg, os.path.join(releases_assets_dir, 'logo.jpg'))
    print("Updated releases/windows/publish/Assets")

print("All rounded logo assets updated successfully!")

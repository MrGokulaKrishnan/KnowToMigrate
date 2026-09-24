import os
from PIL import Image

src_path = r"C:\Users\gokul\.gemini\antigravity\brain\7c9cd54f-bc67-4ceb-b7cf-ac000f3bcb91\.user_uploaded\media_1790243023948.jpg"
root_dir = r"c:\KnowToMigrate"

print(f"Loading master logo from: {src_path}")
master_img = Image.open(src_path).convert("RGBA")

# 1. Root assets
os.makedirs(os.path.join(root_dir, "assets"), exist_ok=True)
master_img.convert("RGB").save(os.path.join(root_dir, "assets", "logo.jpg"), quality=98)
master_img.save(os.path.join(root_dir, "assets", "logo.png"))
print("Saved root assets/logo.jpg and logo.png")

# 2. Website assets
web_public = os.path.join(root_dir, "apps", "website", "public")
os.makedirs(web_public, exist_ok=True)
master_img.convert("RGB").save(os.path.join(web_public, "logo.jpg"), quality=98)
master_img.save(os.path.join(web_public, "logo.png"))

# Favicon
fav_64 = master_img.resize((64, 64), Image.Resampling.LANCZOS)
fav_64.save(os.path.join(web_public, "favicon.png"))
fav_64.save(os.path.join(web_public, "favicon.ico"), format="ICO", sizes=[(64, 64), (32, 32), (16, 16)])
print("Saved website public assets & favicons")

# 3. Windows WPF assets
win_assets = os.path.join(root_dir, "apps", "windows-wpf", "Assets")
os.makedirs(win_assets, exist_ok=True)
master_img.convert("RGB").save(os.path.join(win_assets, "logo.jpg"), quality=98)
master_img.save(os.path.join(win_assets, "logo.png"))

# Windows multi-resolution ICO (256, 128, 64, 48, 32, 16)
ico_sizes = [(256, 256), (128, 128), (64, 64), (48, 48), (32, 32), (16, 16)]
ico_imgs = [master_img.resize(s, Image.Resampling.LANCZOS) for s in ico_sizes]
ico_imgs[0].save(os.path.join(win_assets, "KnowToMigrate.ico"), format="ICO", sizes=ico_sizes)
print("Saved Windows Assets/logo.jpg and KnowToMigrate.ico")

# 4. Android assets
res_dir = os.path.join(root_dir, "apps", "android", "app", "src", "main", "res")
drawable_dir = os.path.join(res_dir, "drawable")
os.makedirs(drawable_dir, exist_ok=True)
master_img.save(os.path.join(drawable_dir, "logo.png"))
print("Saved Android drawable/logo.png")

# Density map for standard launcher icons
density_sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Adaptive foreground sizes (108dp base)
foreground_sizes = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}

for folder, size in density_sizes.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    icon = master_img.resize((size, size), Image.Resampling.LANCZOS)
    icon.save(os.path.join(folder_path, "ic_launcher.png"))
    icon.save(os.path.join(folder_path, "ic_launcher_round.png"))

for folder, size in foreground_sizes.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    fg = master_img.resize((size, size), Image.Resampling.LANCZOS)
    fg.save(os.path.join(folder_path, "ic_launcher_foreground.png"))

print("Saved all Android launcher mipmaps across all densities")
print("SUCCESS: Master logo successfully unified across ALL platforms!")

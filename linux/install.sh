#!/usr/bin/sh
if [ "$EUID" != 0 ]
  then echo 'Root privileges are required to install. You can install the game in your local directory manually.'
  exit
fi
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
echo "Making directory in /usr/share/"
mkdir "/usr/share/combusted-pixel-dungeon"
echo "Installing game..."
cp "$SCRIPT_DIR/combusted-pixel-dungeon.jar" "/usr/share/combusted-pixel-dungeon/"
cp "$SCRIPT_DIR/combusted-pixel-dungeon.png" "/usr/share/combusted-pixel-dungeon/"
echo "Installing binary..."
cp "$SCRIPT_DIR/combusted-pixel-dungeon" "/usr/bin/"
chmod +x "/usr/bin/combusted-pixel-dungeon"
echo "Installing desktop file..."
cp "$SCRIPT_DIR/combusted-pixel-dungeon.desktop" "/usr/share/applications/"

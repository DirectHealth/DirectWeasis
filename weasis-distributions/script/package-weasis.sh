#!/bin/bash
# This script makes the multiplatform build with jpackage
#
# Initial script by Nicolas Roduit

# Build Parameters
REVISON_INC="1"
PACKAGE=YES

# Options
# jdk.localedata => other locale (en_us) data are included in the jdk.localedata
# jdk.jdwp.agent => package for debugging agent

# Base modules for all platforms
JDK_MODULES_BASE="java.base,java.compiler,java.datatransfer,java.net.http,java.desktop,java.logging,java.management,java.prefs,java.xml,jdk.localedata,jdk.charsets,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.jdwp.agent,java.sql"
NAME="DirectViewer"
IDENTIFIER="org.weasis.launcher"

# Aux functions:
die ( ) {
  echo
  echo -e "ERROR: $*"
  exit 1
}

POSITIONAL=()
while [[ $# -gt 0 ]]
do
  key="$1"

  case $key in
    -h|--help)
echo "Usage: package-weasis.sh <options>"
echo "Sample usages:"
echo "    Build an installer for the current platform with the minimal required parameters"
echo "        package-weasis.sh --jdk /home/user/jdk-20"
echo ""
echo "Options:"
echo " --help -h
Print the usage text with a list and description of each valid
option the output stream, and exit"
echo " --input -i
Path of the weasis-native directory"
echo " --output -o
Path of the base output directory.
Default value is the current directory"
echo " --jdk -j
Path of the jdk with the jpackage module"
echo " --temp
Path of the temporary directory during build"
echo " --no-installer
Build only the native binaries not the final installer"
echo " --mac-signing-key-user-name
Key user name of the certificate to sign the bundle"
exit 0
;;
-j|--jdk)
JDK_PATH_UNIX="$2"
shift # past argument
shift # past value
;;
-i|--input)
INPUT_PATH="$2"
shift # past argument
shift # past value
;;
-o|--output)
OUTPUT_PATH="$2"
shift # past argument
shift # past value
;;
--temp)
TEMP_PATH="$2"
shift # past argument
shift # past value
;;
--no-installer)
PACKAGE="NO"
shift # past argument
;;
--mac-signing-key-user-name)
CERTIFICATE="$2"
shift # past argument
shift # past value
;;
*)    # unknown option
POSITIONAL+=("$1") # save it in an array for later
shift # past argument
;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters


curPath=$(dirname "$(readlink -f "$0")")
rootdir="$(dirname "$curPath")"
rootdir="$(dirname "$rootdir")"

echo "rootdir: $rootdir"

if [ ! -d "${INPUT_PATH}" ] ; then
  INPUT_PATH="${rootdir}/bin-dist"
  if [ ! -d "${INPUT_PATH}" ] ; then
    INPUT_PATH="${rootdir}/weasis-distributions/target/native-dist/weasis-native/bin-dist"
  fi
fi

if [ ! -d "${INPUT_PATH}" ] ; then
  die "The input path ${INPUT_PATH} doesn't exist, provide a valid value for --input"
fi

cp "$INPUT_PATH/weasis/bundle/weasis-core-img-"* weasis-core-img.jar.xz
xz --decompress weasis-core-img.jar.xz
ARC_OS=$("$JDK_PATH_UNIX/bin/java" -cp "weasis-core-img.jar" org.weasis.core.util.NativeLibrary)
rm -f weasis-core-img.jar
if [ -z "$ARC_OS" ] ; then
  die "Cannot get Java system architecture"
fi
machine=$(echo "${ARC_OS}" | cut -d'-' -f1)
arc=$(echo "${ARC_OS}" | cut -d'-' -f2-3)

# Set JDK modules based on the platform
if [ "$machine" = "windows" ] ; then
  JDK_MODULES="${JDK_MODULES_BASE},jdk.crypto.mscapi"
else
  JDK_MODULES="${JDK_MODULES_BASE}"
fi

echo "Platform: $machine"
echo "JDK Modules: $JDK_MODULES"

if [ "$machine" = "windows" ] ; then
  INPUT_PATH_UNIX=$(cygpath -u "$INPUT_PATH")
  OUTPUT_PATH_UNIX=$(cygpath -u "$OUTPUT_PATH")
  RES="${curPath}\resources\\${machine}"
else
  INPUT_PATH_UNIX="$INPUT_PATH"
  OUTPUT_PATH_UNIX="$OUTPUT_PATH"
  RES="${curPath}/resources/$machine"
fi

# Set custom JDK path (>= JDK 11)
export JAVA_HOME=$JDK_PATH_UNIX

WEASIS_VERSION=$(grep -i "weasis.version=" "${curPath}/build.properties" | sed 's/^.*=//')

echo System        = "${ARC_OS}"
echo JDK path        = "${JDK_PATH_UNIX}"
echo Weasis version  = "${WEASIS_VERSION}"
echo Input path      = "${INPUT_PATH}"
if [ "$machine" = "windows" ]
then
  echo Input unix path      = "${INPUT_PATH_UNIX}"
fi

# Specify the required Java version.
# Only major version is checked. Minor version or any other version string info is left out.
REQUIRED_TEXT_VERSION=$(grep -i "jdk.version=" "${curPath}/build.properties" | sed 's/^.*=//')
# Extract major version number for comparisons from the required version string.
# In order to do that, remove leading "1." if exists, and minor and security versions.
REQUIRED_MAJOR_VERSION=$(echo $REQUIRED_TEXT_VERSION | sed -e 's/^1\.//' -e 's/\..*//')

# Check jlink command.
if [ -x "$JDK_PATH_UNIX/bin/jpackage" ] ; then
  JPKGCMD="$JDK_PATH_UNIX/bin/jpackage"
  JAVACMD="$JDK_PATH_UNIX/bin/java"
else
  die "JAVA_HOME is not set and no 'jpackage' command could be found in your PATH. Specify a jdk path >=$REQUIRED_TEXT_VERSION."
fi

# Then, get the installed version
INSTALLED_VERSION=$($JAVACMD -version 2>&1 | awk '/version [0-9]*/ {print $3;}')
echo "Found java version $INSTALLED_VERSION"
echo "Java command path: $JAVACMD"

# Remove double quotes, remove leading "1." if it exists and remove everything apart from the major version number.
INSTALLED_MAJOR_VERSION=$(echo "$INSTALLED_VERSION" | sed -e 's/"//g' -e 's/^1\.//' -e 's/\..*//' -e 's/-.*//')
echo "Java major version: $INSTALLED_MAJOR_VERSION"
if (( INSTALLED_MAJOR_VERSION < REQUIRED_MAJOR_VERSION )) ; then
  die "Your version of java is too low to run this script.\nPlease update to $REQUIRED_TEXT_VERSION or higher"
fi

if [ -z "$OUTPUT_PATH" ] ; then
  OUTPUT_PATH="weasis-$ARC_OS-jdk$INSTALLED_MAJOR_VERSION-$WEASIS_VERSION"
  OUTPUT_PATH_UNIX="$OUTPUT_PATH"
fi


if [ "$machine" = "windows" ] ; then
  INPUT_DIR="$INPUT_PATH\weasis"
  IMAGE_PATH="$OUTPUT_PATH\\${NAME}"
else
  IMAGE_PATH="$OUTPUT_PATH/$NAME"
  INPUT_DIR="$INPUT_PATH_UNIX/weasis"
fi

WEASIS_CLEAN_VERSION=$(echo "$WEASIS_VERSION" | sed -e 's/"//g' -e 's/-.*//' -e 's/\(\([0-9]\+\.\)\{2\}[0-9]\+\)\.[0-9]\+/\1/')


# Remove pack jar for launcher
rm -f "$INPUT_DIR"/*.jar.pack.gz

# Remove the unrelated native packages
find "$INPUT_DIR"/bundle/weasis-opencv-core-* -type f ! -name '*-'"${ARC_OS}"'-*'  -exec rm -f {} \;
find "$INPUT_DIR"/bundle/jogamp-* -type f ! -name '*-'"${ARC_OS}"'-*' ! -name 'jogamp-[0-9]*' -exec rm -f {} \;

if [ "$machine" = "macosx" ] ; then
    mkdir jar_contents
    unzip "$INPUT_DIR"/weasis-launcher.jar -d jar_contents
    codesign --force --deep --timestamp --sign "$CERTIFICATE" -vvv jar_contents/com/formdev/flatlaf/natives/libflatlaf-macos-arm64.dylib
    codesign --force --deep --timestamp --sign "$CERTIFICATE" -vvv jar_contents/com/formdev/flatlaf/natives/libflatlaf-macos-x86_64.dylib
    jar cfv weasis-launcher.jar -C jar_contents .
    mv -f weasis-launcher.jar "$INPUT_DIR"/weasis-launcher.jar
    rm -rf jar_contents
fi

# Remove previous package
if [ -d "${OUTPUT_PATH}" ] ; then
  rm -rf "${OUTPUT_PATH}"
fi

if [ -z "$TEMP_PATH" ] ; then
  declare -a tmpArgs=()
else
  declare -a tmpArgs=("--temp" "$TEMP_PATH")
fi

if [ -d "${TEMP_PATH}" ] ; then
  rm -rf "${TEMP_PATH}"
fi

if [ "$machine" = "macosx" ] ; then
  DICOMIZER_CONFIG="Dicomizer=$RES/dicomizer-launcher.properties"
  declare -a customOptions=("--java-options" "-splash:\$APPDIR/resources/images/about-round.png" "--java-options" "-Dapple.laf.useScreenMenuBar=true" "--java-options" "-Dapple.awt.application.appearance=NSAppearanceNameDarkAqua")
  if [[ -n "$CERTIFICATE" ]] ; then
    declare -a signArgs=("--mac-package-identifier" "$IDENTIFIER" "--mac-signing-key-user-name" "$CERTIFICATE"  "--mac-sign")
  else
    declare -a signArgs=("--mac-package-identifier" "$IDENTIFIER")
  fi
elif [ "$machine" = "windows" ] ; then
  DICOMIZER_CONFIG="Dicomizer=$RES\dicomizer-launcher.properties"
  declare -a customOptions=("--java-options" "-splash:\$APPDIR\resources\images\about-round.png" )
  declare -a signArgs=()
else
  DICOMIZER_CONFIG="Dicomizer=$RES/dicomizer-launcher.properties"
  declare -a customOptions=("--java-options" "-splash:\$APPDIR/resources/images/about-round.png" )
  declare -a signArgs=()
fi
declare -a commonOptions=("--java-options" "-Dgosh.port=17179" \
"--java-options" "--enable-native-access=ALL-UNNAMED" \
"--java-options" "-Djavax.accessibility.assistive_technologies=org.weasis.launcher.EmptyAccessibilityProvider" \
"--java-options" "-Djavax.accessibility.screen_magnifier_present=false");

$JPKGCMD --type app-image --input "$INPUT_DIR" --dest "$OUTPUT_PATH" --name "$NAME" \
--main-jar weasis-launcher.jar --main-class org.weasis.launcher.AppLauncher --add-modules "$JDK_MODULES" \
--add-launcher "${DICOMIZER_CONFIG}" --resource-dir "$RES"  --app-version "$WEASIS_CLEAN_VERSION" \
"${tmpArgs[@]}" --verbose "${signArgs[@]}" "${customOptions[@]}" "${commonOptions[@]}"

if [ "$machine" = "macosx" ] && [[ -n "$CERTIFICATE" ]] ; then
    codesign --timestamp --entitlements "$RES/uri-launcher.entitlements" --options runtime --force -vvv --sign "$CERTIFICATE" "$RES/$NAME.app"
fi

if [ "$PACKAGE" = "YES" ] ; then
  VENDOR="DirectHealth"
  COPYRIGHT="© 2023 DirectHealth"
  if [ "$machine" = "windows" ] ; then
    [ "$arc" = "aarch64" ]  && UPGRADE_UID="3aedc24e-48a8-4623-ab39-0c3c01c7383c" || UPGRADE_UID="3aedc24e-48a8-4623-ab39-0c3c01c7383a"
    $JPKGCMD --type "msi" --app-image "$IMAGE_PATH" --dest "$OUTPUT_PATH" --name "$NAME" --resource-dir "$RES/msi/${arc}" \
    --license-file "$INPUT_PATH\Licence.txt" --description "Weasis DICOM viewer" --win-upgrade-uuid "$UPGRADE_UID"  \
    --win-menu --win-menu-group "$NAME" --copyright "$COPYRIGHT" --app-version "$WEASIS_CLEAN_VERSION" \
    --vendor "$VENDOR" --file-associations "${curPath}\file-associations.properties" "${tmpArgs[@]}" --verbose
    mv "$OUTPUT_PATH_UNIX/$NAME-$WEASIS_CLEAN_VERSION.msi" "$OUTPUT_PATH_UNIX/$NAME-$WEASIS_CLEAN_VERSION-${arc}.msi"
  elif [ "$machine" = "linux" ] ; then
    declare -a installerTypes=("deb" "rpm")
    for installerType in "${installerTypes[@]}"; do
      [ "${installerType}" = "rpm" ] && DEPENDENCIES="" || DEPENDENCIES="libstdc++6, libgcc1"
      $JPKGCMD --type "$installerType" --app-image "$IMAGE_PATH" --dest "$OUTPUT_PATH"  --name "$NAME" --resource-dir "$RES" \
      --license-file "$INPUT_PATH/Licence.txt" --description "Weasis DICOM viewer" --vendor "$VENDOR" \
      --copyright "$COPYRIGHT" --app-version "$WEASIS_CLEAN_VERSION" --file-associations "${curPath}/file-associations.properties" \
      --linux-app-release "$REVISON_INC" --linux-package-name "weasis" --linux-deb-maintainer "Nicolas Roduit" --linux-rpm-license-type "EPL-2.0" \
      --linux-menu-group "Viewer;MedicalSoftware;Graphics;" --linux-app-category "science" --linux-package-deps "${DEPENDENCIES}" \
      --linux-shortcut "${tmpArgs[@]}" --verbose
      if [ -d "${TEMP_PATH}" ] ; then
        rm -rf "${TEMP_PATH}"
      fi
    done
  elif [ "$machine" = "macosx" ] ; then
    $JPKGCMD --type "pkg" --app-image "$IMAGE_PATH.app" --dest "$OUTPUT_PATH" --name "$NAME" --resource-dir "$RES" \
    --license-file "$INPUT_PATH/Licence.txt" --copyright "$COPYRIGHT" --app-version "$WEASIS_CLEAN_VERSION" \
    "${tmpArgs[@]}" --verbose "${signArgs[@]}"
  fi
fi
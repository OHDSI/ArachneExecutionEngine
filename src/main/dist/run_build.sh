#!/usr/bin/env bash

DIST=trusty
ARCH=amd64
BUILD_PATH=./dist
WS=`dirname $0`

function print_help {
	echo "Usage: run_build.sh [OPTIONS]"
	echo "Available options are:"
	echo -e "  -a i386|amd64 \tDistribution architecture, default is amd64"
	echo -e "  -d DIST_NAME \t\tUbuntu distribution name, e.g. trusty or xenial, default is trusty"
	echo -e "  -b BUILDDIR \t\tDirectory where distribution build would be running"
	echo -e "  -f FILE \t\tOutput archive filename"
	echo -e "  -h \t\t\tPrints this"
}

OPTIND=1
while getopts ":a:d:b:f:h" opt; do
	case $opt in 
		a)
			ARCH=$OPTARG
			;;
		d)	
			DIST=$OPTARG
			;;
		b)
			BUILD_PATH=$OPTARG
			;;
		f)
			ARCHIVE=$OPTARG
			;;
		h)
			print_help
			exit 0
			;;
		\?)
			echo "Invalid option: -$OPTARG" >&2
			exit 1
			;;
		:)
			echo "Option -$OPTARG requires an argument." >&2
			exit 1
			;;
	esac
done

if [[ -z $ARCHIVE ]]; then
	ARCHIVE=../r_base_${DIST}_${ARCH}.tar.gz
fi

if [[ ! -d $BUILD_PATH ]]; then
	mkdir -p $BUILD_PATH
fi

if [[ "$(ls -A $BUILD_PATH)" ]]; then
	echo "$BUILD_PATH is not empty, woun't continue"
	exit 1
fi

echo "Starting build distribution."
echo "Release: $DIST $ARCH"
echo "Build dir: $BUILD_PATH"
echo "Output file: $ARCHIVE"
echo ""

debootstrap --arch amd64 trusty $BUILD_PATH http://ubuntu.cs.utah.edu/ubuntu/
mount --bind /proc $BUILD_PATH/proc

cp $WS/install_packages.sh $BUILD_PATH/root/
cp $WS/../docker/libs.r $BUILD_PATH/root/

sudo chmod +x $BUILD_PATH/root/install_packages.sh
sudo chroot $BUILD_PATH /root/install_packages.sh $DIST

umount $BUILD_PATH/proc
rm $BUILD_PATH/root/install_packages.sh
rm $BUILD_PATH/root/libs.r
cd $BUILD_PATH

tar czf $ARCHIVE .
echo "Distribution Archive built and available at $ARCHIVE"
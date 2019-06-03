#!/usr/bin/env bash

DIST=trusty
ARCH=amd64
BUILD_PATH=./dist
WS=`dirname $0`

BQ_PATH=../extras/bigquery/
IMPALA_PATH=../extras/impala/
NETEZZA_PATH=../extras/netezza/

function print_help {
	echo "Usage: run_build.sh [OPTIONS]"
	echo "Available options are:"
	echo -e "  -a i386|amd64 \tDistribution architecture, default is amd64"
	echo -e "  -d DIST_NAME \t\tUbuntu distribution name, e.g. trusty or xenial, default is trusty"
	echo -e "  -b BUILDDIR \t\tDirectory where distribution build would be running"
	echo -e "  -f FILE \t\tOutput archive filename"
	echo -e "  -bq PATH \t\tPath to BigQuery drivers"
	echo -e "  -impala PATH \t\tPath to Impala drivers"
	echo -e "  -netezza PATH \t\tPath to Netezza drivers"
	echo -e "  -h \t\t\tPrints this"
}

OPTIND=1
while getopts ":a:d:b:f:h:bq:impala:netezza" opt; do
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
		bq)
		    BQ_PATH=$OPTARG
		    ;;
		impala)
		    IMPALA_PATH=$OPTARG
		    ;;
		netezza)
		    NETEZZA_PATH=$OPTARG
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

# Download libs.r from GitHub repo
if [[ -f "libs.r" ]]; then
    rm -f "libs.r"
fi
curl https://raw.githubusercontent.com/odysseusinc/DockerEnv/master/libs.r -o libs.r

debootstrap --arch amd64 $DIST $BUILD_PATH http://ubuntu.cs.utah.edu/ubuntu/
mount --bind /proc $BUILD_PATH/proc

cp $WS/install_packages.sh $BUILD_PATH/root/
cp $WS/libs.r $BUILD_PATH/root/
#Impala drivers
mkdir $BUILD_PATH/impala/
cp $IMPALA_PATH/*.jar $BUILD_PATH/impala/
cp ../docker/krb5.conf $BUILD_PATH/etc/

# BigQuery drivers
mkdir $BUILD_PATH/bigquery/
cp $BQ_PATH/*.jar $BUILD_PATH/bigquery/

# Netezza drivers
mkdir $BUILD_PATH/netezza/
cp $NETEZZA_PATH/*.jar $BUILD_PATH/netezza/

sudo chmod +x $BUILD_PATH/root/install_packages.sh
sudo chroot $BUILD_PATH /root/install_packages.sh $DIST

umount $BUILD_PATH/proc
rm $BUILD_PATH/root/install_packages.sh
rm $BUILD_PATH/root/libs.r
cd $BUILD_PATH

# To prevent unexpected package updates
cp $WS/.Rprofile $BUILD_PATH/root/

tar czf $ARCHIVE .
echo "Distribution Archive built and available at $ARCHIVE"
package kr.co.iteyes.fhir.jpa.security.util;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Lz4Utils {
    static final int DEFAULT_COMPRESSION_LEVEL = 9;
    static final int MINIMUM_COMPRESSION_LENGTH = 128;
    static final int DEFAULT_BUFFER_LENGTH = 1;
    static final int MAXIMUM_BUFFER_LENGTH = 8 * 1024 * 1024;
    static final byte[] MYMDLZ4_MAGIC = {(byte) 0x83, (byte) 0xe2, (byte) 0xb9};
    static final int MYMDLZ4_MAGIC_LENGTH = MYMDLZ4_MAGIC.length;
    static final int MYMDLZ4_HEADER_LENGTH = MYMDLZ4_MAGIC_LENGTH + Integer.BYTES;
    static final int POSSIBLE_MAXIMUM_LENGTH = 512 * 1024 * 1024;

    private LZ4Compressor lz4Compressor;
    private LZ4SafeDecompressor lz4SafeDecompressor;
    private byte[] compressBuffer = new byte[DEFAULT_BUFFER_LENGTH];
    private byte[] decompressBuffer = new byte[DEFAULT_BUFFER_LENGTH];


    /**
     * 기본 압축레벨(9)로 인스턴스를 생성한다.
     *
     */
    public Lz4Utils() {
        this(DEFAULT_COMPRESSION_LEVEL);
    }

    /**
     * 압축레벨 지정하여 인스턴스를 생성한다.
     *
     * @param level 압축레벨으로 [9,17]의 값을 가진다. 높은 값이 압축률이 높다.
     */
    public Lz4Utils(int level) {
        LZ4Factory lz4Factory = LZ4Factory.fastestInstance();
        lz4Compressor = lz4Factory.highCompressor(level);
        lz4SafeDecompressor = lz4Factory.safeDecompressor();
    }

    /**
     * LZ4 포맷을 구성한다.
     *
     * @param src 압축된 바이트 배열
     * @param offset 압축된 바이트 배열 상의 오프셋
     * @param length 압축된 길이
     * @param orgLength 압축 전의 길이
     * @return LZ4 포맷으로 구성된 바이트 배열
     */
    private byte[] compositeLz4(final byte[] src, int offset, int length, int orgLength) {
        ByteBuffer buffer = ByteBuffer.allocate(length + MYMDLZ4_HEADER_LENGTH);

        buffer.put(MYMDLZ4_MAGIC);
        buffer.putInt(orgLength);
        buffer.put(src, offset, length);

        return buffer.array();
    }

    /**
     * 바이트 배열을 압축한다.
     *
     * @param src 압축할 바이트 배열
     * @param offset 압축을 시작하는 배열 상의 오프셋
     * @param length 압축할 길이
     * @return 압축된 바이트 배열
     */
    public byte[] compress(final byte[] src, int offset, int length)
            throws NullPointerException, IllegalArgumentException, ArrayIndexOutOfBoundsException, LZ4Exception {
        // 파라메터 검사
        if (src == null) {
            throw new NullPointerException("src couldn't be null");
        } else if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0, got " + offset);
        } else if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0, got " + length);
        } else if (length > POSSIBLE_MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("length is too long. it couldn't exceed " + POSSIBLE_MAXIMUM_LENGTH);
        } else if ((length - offset) > src.length) {
            throw new ArrayIndexOutOfBoundsException("offset + length couldn't exceed src length");
        }

        // 최소 압축 길이가 안 되면 압축하지 않는다.
        if (length <= MINIMUM_COMPRESSION_LENGTH) {
            return compositeLz4(src, offset, length, length);
        }

        // 압축용 버퍼 생성
        final int maxCompressedLength = lz4Compressor.maxCompressedLength(length);
        if (compressBuffer.length < maxCompressedLength) {
            compressBuffer = new byte[maxCompressedLength];
        }

        // 압축용 버퍼 생성
        int compressedLength = lz4Compressor.compress(src, offset, length, compressBuffer, 0, maxCompressedLength);

        // 압축한 크기가 더 크면 src를 반환한다.
        int ratio = (length * 100) / compressedLength;

        if (ratio < 110) {
            return compositeLz4(src, offset, length, length);
        }

        byte[] compressed = compositeLz4(compressBuffer, 0, compressedLength, length);

        if (compressBuffer.length > MAXIMUM_BUFFER_LENGTH) {
            compressBuffer = new byte[DEFAULT_BUFFER_LENGTH];
        }

        return compressed;
    }

    /**
     * 바이트 배열을 압축한다.
     *
     * @param src 압축할 바이트 배열
     * @return 압축된 바이트 배열
     */
    public byte[] compress(final byte[] src)
            throws NullPointerException, IllegalArgumentException, ArrayIndexOutOfBoundsException, LZ4Exception {
        return compress(src, 0, src.length);
    }


    /**
     * 압축된 바이트 배열을 해제한다.
     *
     * @param src 해제할 바이트 배열
     * @param offset 해제를 시작하는 배열 상의 오프셋
     * @return 해제된 바이트 배열
     */
    public byte[] decompress(final byte[] src, int offset, int length)
            throws NullPointerException, IllegalArgumentException, ArrayIndexOutOfBoundsException, LZ4Exception {
        if (src == null) {
            throw new NullPointerException("src couldn't be null");
        } else if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0, got " + offset);
        } else if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0, got " + length);
        } else if ((length - offset) > src.length) {
            throw new ArrayIndexOutOfBoundsException("offset + length couldn't exceed src length");
        }

        // 헤더보다 짧으면 압축되지 않은 것으로 전체를 복사해서 리턴한다.
        if (length <= MYMDLZ4_MAGIC_LENGTH) {
            throw new IllegalArgumentException("Malformed! src is shorter than length of header.");
        }

        int orgLength;
        {
            ByteBuffer buffer = ByteBuffer.wrap(src);

            byte[] checkMagic = new byte[MYMDLZ4_MAGIC_LENGTH];
            buffer.get(checkMagic);

            // 헤더가 MYMDLZ4_MAGIC로 시작하지 않으면 압축되지 않은 것으로 전체를 복사해서 리턴한다.
            if (!Arrays.equals(MYMDLZ4_MAGIC, checkMagic)) {
                throw new IllegalArgumentException("Malformed! src doesn't match header.");
            }

            orgLength = buffer.getInt();

            if (orgLength < 0 || orgLength > POSSIBLE_MAXIMUM_LENGTH) {
                throw new IllegalArgumentException("Malformed! original length(" + orgLength + ") is abnormal.");
            }
        }

        length -= MYMDLZ4_HEADER_LENGTH;
        offset += MYMDLZ4_HEADER_LENGTH;
        if (length == orgLength) {
            return Arrays.copyOfRange(src, offset, offset + length);
        }

        if (decompressBuffer.length < orgLength) {
            decompressBuffer = new byte[orgLength];
        }

        int decompressedLength = lz4SafeDecompressor.decompress(src, offset, length, decompressBuffer, 0);

        byte[] decompressed = Arrays.copyOfRange(decompressBuffer, 0, decompressedLength);

        if (decompressBuffer.length > MAXIMUM_BUFFER_LENGTH) {
            decompressBuffer = new byte[DEFAULT_BUFFER_LENGTH];
        }

        return decompressed;
    }

    /**
     * 압축된 바이트 배열을 해제한다.
     *
     * @return 해제된 바이트 배열
     */
    public byte[] decompress(final byte[] src)
            throws NullPointerException, IllegalArgumentException, ArrayIndexOutOfBoundsException, LZ4Exception {
        return decompress(src, 0, src.length);
    }
}

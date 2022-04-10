package osmreader;

public class ZCurveIDStrategy implements IDStrategy {
    @Override
    public String generateID(String id, String lat, String lon) {
        int latBits = (int) Double.parseDouble(lat) * 10000000;
        int lonBits = (int) Double.parseDouble(lon) * 10000000;
        return Long.toString(interleave(latBits, lonBits));
    }
    public long interleave(int left, int right) {
        long result = 0;
        for (int index = 0; index < Long.SIZE; index++) {
            long left_masked_index = (left & (1L << index));
            long right_masked_index = (right & (1L << index));
            result |= (left_masked_index << index);
            result |= (right_masked_index << (index + 1));
        }
        return result;
    }
}

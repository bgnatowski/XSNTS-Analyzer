package pl.bgnat.master.xsnts.exporter.service.exporters;

public interface Exporter {
    default String export(String userPath) throws Exception {
        throw new UnsupportedOperationException("Not supported");
    }
    default String export(Long id, String userPath) throws Exception {
        throw new UnsupportedOperationException("Not supported");
    }
}


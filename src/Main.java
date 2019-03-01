import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Main {
    private  static final String outPatch="out.txt";
    private static Path paths=Paths.get(outPatch);
    private static String in;
    private static boolean isFile;

    public static void main(String[] args) {

        if(args.length>0){
            isFile=true;
        }else {
            try {
                printable();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
        }

    }

    static void printable() throws IOException {

        File f=new File(outPatch);
        if(f.exists()){
            f.delete();
        }
        File classpathRoot = new File(System.getProperty("user.dir"));

        File[] temp = classpathRoot.listFiles(new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.endsWith(".txt");
            }
        });
        if(temp.length==0){
            addFileOut("Не найден текстовый файл с серверными строками");
            return;
        }
        if(temp.length>2){
            addFileOut("существует несколько файлов с сервреными строкаи.");
            for (File c : temp) {
                System.out.println(c.getName());
            }
            return;
        }
        in=temp[0].getName();
        File[] cexcelFiles = classpathRoot.listFiles(new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return name.endsWith(".xlsx");
            }
        });
        Files.write(paths,("Проверка соответствия "+new Date().toString()).getBytes());
        if(cexcelFiles.length==0){
            addFileOut("не могу найти файл столото.");
            return;
        }
        if(cexcelFiles.length>1){
            addFileOut("не могу определеить файл столото, их больше чем один.");
            for (File c : cexcelFiles) {
                System.out.println(c.getName());
            }
            return;
        }
        FileInputStream file = new FileInputStream(cexcelFiles[0]);
        addFileOut("Парсинг данных с столото.");
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);
        Map<Integer, List<String>> data = new HashMap<>();
        int i = -1;
        for (Row row : sheet) {
            i++;
            data.put(i, new ArrayList<>());
            for (Cell cell : row) {
                switch (cell.getCellTypeEnum()) {
                    case STRING: {
                        data.get(new Integer(i)).add(cell.getRichStringCellValue().getString());
                        break;
                    }
                    case NUMERIC: {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            data.get(i).add(cell.getDateCellValue() + "");
                        } else {
                            data.get(i).add(cell.getNumericCellValue() + "");
                        }
                        break;
                    }
                    default: data.get(new Integer(i)).add(" ytn");
                }
            }
        }
        List<DataLoto> dataLotos=new ArrayList<>(data.size());
        for (Map.Entry<Integer, List<String>> dd : data.entrySet()) {
            if(dd.getValue().size()!=19) continue;
            if(dd.getValue().get(1).contains(":")==false) continue;
           try{
                DataLoto dl=new DataLoto();
                dl.date=dd.getValue().get(0);
                dl.time=dd.getValue().get(1);
                dl.type=dd.getValue().get(3);
                dl.agent=dd.getValue().get(4);;
                dl.terminal= getInteger(dd.getValue().get(5));
                dl. external_id=Long.parseLong(dd.getValue().get(8));
                dl.game=dd.getValue().get(9);
                dl.key=getInteger(dd.getValue().get(10));
                dl.barcode=dd.getValue().get(11);;
                dl.tirag=getInteger(dd.getValue().get(12));
                dl.ticket_number=getInteger(dd.getValue().get(13));
                dl.first=getInteger(dd.getValue().get(14));
                dl.last=getInteger(dd.getValue().get(15));
                dl.summa=Double.parseDouble(dd.getValue().get(16));
                dl.telephone=dd.getValue().get(17);;
                dl.sign=getInteger(dd.getValue().get(18));
                dataLotos.add(dl);
            }catch (Exception ex){
              System.out.println("Error");
              System.out.println(String.join("#",dd.getValue()));
              throw ex;
            }
        }
        addFileOut("Формирование промежуточных данных. записей:"+dataLotos.size());
        Map<String,DataLoto> result=new HashMap<>(dataLotos.size());
        for (DataLoto dataLoto : dataLotos) {
            if(dataLoto.sign==0){
                result.put(""+dataLoto.terminal+"/"+dataLoto.external_id,dataLoto);
            }
        }
        addFileOut("Формирование данных у которых сигнатура ответа = 0. записей:"+result.size());
        List<String> list=Files.readAllLines(Paths.get(in));
        Map<String,String> mapServer=new HashMap<>(list.size());
        addFileOut("Парсинг серверных данных bitnic");
        for (String s : list) {
            if(s.contains(";")==false) continue;
            String r[]=s.split(";",-1);
            String d=r[49];
            if(r[50].length()>0){
                d=r[50];
            }
            if(d.length()==0)continue;
            Map<String,String> keys=getMapResponse(d);
            String ss=keys.get("TERMINAL_ID");
            String ss1=keys.get("EXTERNAL_TRANSACTION_ID");
            String keyCore=ss+"/"+ss1;
            if(mapServer.containsKey(keyCore)==false){
                mapServer.put(keyCore,s);
            }else {
                addFileOut("Задваивание серверных транзакций");
                addFileOut(s);
            }
        }
        addFileOut("Формирование данных c сервера. записей:"+mapServer.size());
        addFileOut("Проверка соответствия столото -> сервер");
        for (Map.Entry<String, DataLoto> sdd : result.entrySet()) {
            if(mapServer.containsKey(sdd.getKey())){
                addFileOut("OK    stoloto: " + sdd.getValue().date+" "+sdd.getValue().time+ " "+ sdd.getValue().terminal +" "+ sdd.getValue().external_id +" bitnic: "+mapServer.get(sdd.getKey()));
            }else {
                addFileOut(System.lineSeparator());
                addFileOut("NO    stoloto: " + sdd.getValue().date+" "+sdd.getValue().time+ " "+ sdd.getValue().terminal +" "+ sdd.getValue().external_id+" bitnic: "+mapServer.get(sdd.getKey()));
                addFileOut(System.lineSeparator());
            }
        }
        addFileOut("Сравнение закончено. сравнено "+result.size()+" записей с сервера столото и "+mapServer.size()+" записей с сервера bitnic");
    }

    static Integer  getInteger(String s){
        if(s.trim().length()==0){
            return null;
        }
        else {
           return (int) Double.parseDouble(s);
        }
    }

    public static Map<String, String> getMapResponse(String s) {
        String[] res=s.split("&",-1);
        Map<String, String> map=new HashMap<>(res.length);
        for (String re : res) {
            String d[]=re.split("=");
            map.put(d[0],d[1]);
        }
        return map;
    }

    static class DataLoto{
        public String date;
        public String time;
        public String type;
        public String agent;
        public Integer terminal;
        public long external_id;
        public String game;
        public Integer key;
        public String barcode;
        public Integer tirag;
        public Integer ticket_number;
        public Integer first;
        public Integer last;
        public double summa;
        public String telephone;
        public Integer sign;
    }

    static void addFileOut(String s) throws IOException {
        System.out.println(s);
        Files.write(paths,(System.lineSeparator()+s).getBytes(), StandardOpenOption.APPEND);
    }
}

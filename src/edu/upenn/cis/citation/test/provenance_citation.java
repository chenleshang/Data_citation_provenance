package edu.upenn.cis.citation.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONException;
import edu.upenn.cis.citation.Corecover.Query;
import edu.upenn.cis.citation.Corecover.Subgoal;
import edu.upenn.cis.citation.Corecover.Tuple;
import edu.upenn.cis.citation.citation_view.Head_strs;
import edu.upenn.cis.citation.citation_view.citation_view_vector;
import edu.upenn.cis.citation.examples.Load_views_and_citation_queries;
import edu.upenn.cis.citation.init.init;
import edu.upenn.cis.citation.pre_processing.view_operation;
import edu.upenn.cis.citation.prov_reasoning.Prov_reasoning4;
import edu.upenn.cis.citation.query.Query_provenance;
import edu.upenn.cis.citation.user_query.query_storage;
import edu.upenn.cis.citation.views.Single_view;

public class provenance_citation {
  
//  static String path = "/home/wuyinjun/workspace/Data_citation_demo/reasoning_results/";
  
  static String path = "reasoning_results/";
  
  public static Vector<Head_strs> tuple_why_prov_mappings = new Vector<Head_strs>();
  
  public static ArrayList<Vector<Head_strs>> all_why_tokens = new ArrayList<Vector<Head_strs>>();

  
  public static void main(String [] args) throws SQLException, ClassNotFoundException, IOException, InterruptedException, JSONException
  {
    Query query = load_query();
    
    use_reasoning4(args);
//    use_reasoning3(args, query);
    
  }
  
  
  static Query load_query() throws SQLException, ClassNotFoundException
  {
    
    Connection c = null;
    
    Class.forName("org.postgresql.Driver");
    c = DriverManager
        .getConnection(init.db_url, init.usr_name , init.passwd);
    
    PreparedStatement pst = null;
    
    Query query = Load_views_and_citation_queries.get_views("query", c, pst).get(0);
//    Query query = query_storage.get_query_by_id(1, c, pst);
    
    c.close();
    
    return query;

  }
  
  static void use_reasoning4(String [] args) throws ClassNotFoundException, SQLException, IOException, InterruptedException, JSONException
  {
    Connection c = null;
    PreparedStatement pst = null;
    
  Class.forName("org.postgresql.Driver");
  c = DriverManager
      .getConnection(init.db_url, init.usr_name , init.passwd);
  
//  view_operation.delete_view_by_name("v2", c, pst);
  
//    Query query = query_storage.get_query_by_id(1, c, pst);
  
  Query query = Load_views_and_citation_queries.get_views("query", c, pst).get(0);
    
    boolean iscluster = Boolean.valueOf(args[0]);
    
    boolean sortcluster = false;//Boolean.valueOf(args[1]);
    
    int factor = 2;//Integer.valueOf(args[1]);
    
    Prov_reasoning4.factor = factor;
    
    Prov_reasoning4.sort_cluster = sortcluster;
    
    Prov_reasoning4.init();
  
//    Prov_reasoning4.init_from_database(c, pst);
    
    Prov_reasoning4.init_from_files("views", c, pst);
    
    double start = 0;
    
    double end = 0;
    
    start = System.nanoTime();
    
    Vector<String[]> provenance_instances = Query_provenance.get_provenance_instance();
    
    ConcurrentHashMap<Single_view, HashSet<Tuple>> curr_valid_view_mappings = new ConcurrentHashMap<Single_view, HashSet<Tuple>>();
    
    HashSet<citation_view_vector> covering_sets = Prov_reasoning4.reasoning(query, curr_valid_view_mappings, iscluster, provenance_instances, c, pst);

    end = System.nanoTime();
    
    double time = (end - start)*1.0/1000000000;
    
    if(iscluster)
    {
      System.out.println("reasoning time 3:" + time);
      
      System.out.println("view_mapping_time 3:" + Prov_reasoning4.view_mapping_time);
      
      System.out.println("covering_set_time 3:" + Prov_reasoning4.covering_set_time);

    }
    else
    {
      System.out.println("reasoning time 4:" + time);
      
      System.out.println("view_mapping_time 4:" + Prov_reasoning4.view_mapping_time);
      
      System.out.println("covering_set_time 4:" + Prov_reasoning4.covering_set_time);

    }
    Set<Tuple> view_mappings = Prov_reasoning4.tuple_valid_rows.keySet();
    
    write2file_view_mappings(path + "view_mapping_rows2", Prov_reasoning4.tuple_valid_rows);

    if(iscluster)
      write2file(path + "covering_sets3", covering_sets);
    else
      write2file(path + "covering_sets4", covering_sets);
    
    HashSet<String> formatted_citations = Prov_reasoning4.gen_citations(curr_valid_view_mappings, covering_sets, c, pst);
    
    write2file(path + "citation2", formatted_citations);
    
    write2file(path + "covering_sets_per_group2", Prov_reasoning4.group_covering_sets);
    
    c.close();
  }
  
  static Head_strs get_query_result(ResultSet rs, int head_arg_size) throws SQLException
  {
    Vector<String> values = new Vector<String>();
    
    for(int i = 0; i<head_arg_size; i++)
    {
      String value = rs.getString(i + 1);
      
      values.add(value);
    }
    
    Head_strs curr_query_result = new Head_strs(values);
    
    return curr_query_result;
  }
  
  static Vector<Head_strs> get_tuples(ResultSet rs, Query query) throws SQLException
  {
    Vector<Head_strs> curr_tuples = new Vector<Head_strs>();
    
    Vector<String> provenance = new Vector<String>();
    
//    int total_col_count = provenance_row.length;
    
    int col_nums = query.head.size();
    
    for(int i = 0; i<query.body.size(); i++)
    {
      provenance.clear();
      
      Subgoal subgoal = (Subgoal) query.body.get(i);
      
      for(int j = 0; j<subgoal.args.size(); j++)
      {
        provenance.add(rs.getString(col_nums + 1));
        
        col_nums++;
      }
      
      Head_strs curr_tuple = new Head_strs(provenance);
      
      curr_tuples.add(curr_tuple);
    }
    
//    System.out.println(total_col_count + "::" + col_nums);
    
    return curr_tuples;
    
  }
  
  private static void printResult(ResultSet rs, Query query) throws SQLException, FileNotFoundException, UnsupportedEncodingException {
    
    int rows = 0;
    
    while(rs.next())
    {
      
      Head_strs values = get_query_result(rs, query.head.size());
      
      Vector<Head_strs> curr_tuples = get_tuples(rs, query);
      
      System.out.println(rows);
//      
//      System.out.println(Runtime.getRuntime().totalMemory());
//      
//      System.out.println(Runtime.getRuntime().freeMemory());
//      
//      System.out.println(curr_tuples);
      
      
      
//      if(tuple_why_prov_mappings.get(values) == null)
//      {
//        ArrayList<Integer> curr_tokens = new ArrayList<Integer>();
//        
//        curr_tokens.add(rows);
//        
//        tuple_why_prov_mappings.add(values);
//        
////        System.out.println(values + "::" + curr_tokens);
//        
//      }
//      else
      {
        tuple_why_prov_mappings.add(values);
        
//        System.out.println(values + "::" + tuple_why_prov_mappings.get(values));
      }
      
      all_why_tokens.add(curr_tuples);
      
      rows ++;
      
      
    }
    
    
    
    
  }
  
  static void use_reasoning3(String [] args, Query query) throws ClassNotFoundException, SQLException, IOException, InterruptedException, JSONException
  {
    PreparedStatement pst = null;
    
    boolean iscluster = Boolean.valueOf(args[0]);
    
    boolean sortcluster = false;//Boolean.valueOf(args[1]);
    
    int factor = 2;//Integer.valueOf(args[1]);
    
    Query_provenance.connect(init.db_prov_url, init.usr_name, init.passwd);
    
    Prov_reasoning4.test_case = false;
    
//    Query query = query_storage.get_query_by_id(1, Query_provenance.con, pst);
    
    ResultSet rs = Query_provenance.get_provenance4query(query, Prov_reasoning4.test_case);
    
//    printResult(rs, query);
    
    
    
    Connection c = null;
    
  Class.forName("org.postgresql.Driver");
  c = DriverManager
      .getConnection(init.db_url, init.usr_name , init.passwd);
  
    Prov_reasoning4.factor = factor;
    
    Prov_reasoning4.sort_cluster = sortcluster;
    
    Prov_reasoning4.init();
  
    Prov_reasoning4.init_from_files("views", c, pst);//(c, pst);
    
    double start = 0;
    
    double end = 0;
    
    start = System.nanoTime();
    
    ConcurrentHashMap<Single_view, HashSet<Tuple>> curr_valid_view_mappings = new ConcurrentHashMap<Single_view, HashSet<Tuple>>();
    
//    HashSet<citation_view_vector> covering_sets = new HashSet<citation_view_vector>();
    
    HashSet<citation_view_vector> covering_sets = Prov_reasoning4.reasoning(query, curr_valid_view_mappings, iscluster, rs, c, pst);

    end = System.nanoTime();
    
    double time = (end - start)*1.0/1000000000;
    
    if(iscluster)
    {
      System.out.println("reasoning time 3:" + time);
      
      System.out.println("view_mapping_time 3:" + Prov_reasoning4.view_mapping_time);
      
      System.out.println("covering_set_time 3:" + Prov_reasoning4.covering_set_time);

    }
    else
    {
      System.out.println("reasoning time 4:" + time);
      
      System.out.println("view_mapping_time 4:" + Prov_reasoning4.view_mapping_time);
      
      System.out.println("covering_set_time 4:" + Prov_reasoning4.covering_set_time);

    }
    Set<Tuple> view_mappings = Prov_reasoning4.tuple_valid_rows.keySet();
    
    write2file_view_mappings(path + "view_mapping_rows2", Prov_reasoning4.tuple_valid_rows);

    if(iscluster)
      write2file(path + "covering_sets3", covering_sets);
    else
      write2file(path + "covering_sets4", covering_sets);
    
    HashSet<String> formatted_citations = Prov_reasoning4.gen_citations(curr_valid_view_mappings, covering_sets, c, pst);
    
    write2file(path + "citation2", formatted_citations);
    
    write2file(path + "covering_sets_per_group2", Prov_reasoning4.group_covering_sets);
    
    Query_provenance.reset();
    
    c.close();
  }
  
  
  static void output_view_mapping_valid_rids()
  {
    Set<Tuple> tuples = Prov_reasoning4.tuple_valid_rows.keySet();
    
    for(Tuple tuple: tuples)
    {
      System.out.print(tuple.name + "   " + Prov_reasoning4.tuple_valid_rows.get(tuple).size());
      
      System.out.println();
    }
  }
  
  static void output_view_mappings_per_group()
  {
    Set<String> strings = Prov_reasoning4.group_view_mappings.keySet();
    
    int num = 0;
    
    for(String string: strings)
    {
      System.out.println("group" + num);
      
      ConcurrentHashMap<Tuple, Integer> tuple_indexes = Prov_reasoning4.group_view_mappings.get(string);
      
      Set<Tuple> tuples = tuple_indexes.keySet();
      
      String[] tuple_strs = new String[tuples.size()];
      
      int id = 0;
      
      for(Tuple tuple: tuples)
      {
        tuple_strs[id++] = tuple.name;
        
      }

      Arrays.sort(tuple_strs);
      
      for(String tuple_str: tuple_strs)
      {
        System.out.print(tuple_str + "  ");
      }
      
      System.out.println();
      
      num++;
      
    }
  }
  
  static String get_sorted_mapping_string(Tuple view_mapping, HashMap<String, String> subgoal_name_mappings)
  {
    Set<String> subgoal_names =  view_mapping.mapSubgoals_str.keySet(); 
    
    Vector<String> subgoal_name_list = new Vector<String>(subgoal_names);
    
    Collections.sort(subgoal_name_list);
    
    String sorted_mapping_string = new String();
    
    int count = 0;
    
    for(String subgoal_name: subgoal_name_list)
    {
      if(count >= 1)
        sorted_mapping_string += ",";
      
      sorted_mapping_string += subgoal_name + "=" + subgoal_name_mappings.get(subgoal_name);
      
      count ++;
    }
    
    return sorted_mapping_string;
    
  }
  
  static void write2file_view_mappings(String file_name, ConcurrentHashMap<Tuple, HashSet> view_mapping_count) throws IOException
  {
      File fout = new File(file_name);
      FileOutputStream fos = new FileOutputStream(fout);
   
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
   
      Set<Tuple> view_mappings = view_mapping_count.keySet();
      
      for(Tuple view_mapping: view_mappings)
      {
        int count = view_mapping_count.get(view_mapping).size();
        
        String sorted_mapping_string = get_sorted_mapping_string(view_mapping, view_mapping.mapSubgoals_str);
        
        String view_mapping_str = view_mapping.query.view_name + "|" + sorted_mapping_string + ":" + count;
        
        if(count == 0)
          continue;
        
//        System.out.println(view_mapping_str);
        
        bw.write(view_mapping_str);
        
        bw.newLine();
      }
   
      bw.close();
  }
  
  public static void write2file(String file_name, ConcurrentHashMap<String, HashSet<citation_view_vector>> views) throws IOException
  {
    File fout = new File(file_name);
    FileOutputStream fos = new FileOutputStream(fout);
 
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
 
    Set<String> group_label = views.keySet();
    
    int num = 0;
    
    for(String label: group_label)
    {
      bw.write("group " + num);
      bw.newLine();
      
      HashSet<citation_view_vector> covering_sets = views.get(label);
      
      String [] covering_set_string = new String [covering_sets.size()];
      
      int id = 0;
      
      for(citation_view_vector covering_set: covering_sets)
      {
        covering_set_string[id ++] = covering_set.toString(); 
      }
      
      Arrays.sort(covering_set_string);
      
      for(String covering_set_str: covering_set_string)
      {
        bw.write(covering_set_str);
        bw.newLine();
      }
      
      num++;
      
    }
    
    bw.close();

  }
  
  public static void write2file(String file_name, HashSet views) throws IOException
  {
      File fout = new File(file_name);
      FileOutputStream fos = new FileOutputStream(fout);
   
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
   
      if(views != null && !views.isEmpty())
      for (Object view: views) {
          bw.write(view.toString());
          bw.newLine();
      }
   
      bw.close();
  }

}

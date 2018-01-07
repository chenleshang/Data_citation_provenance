package edu.upenn.cis.citation.views;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import edu.upenn.cis.citation.Corecover.Argument;
import edu.upenn.cis.citation.Corecover.CoreCover;
import edu.upenn.cis.citation.Corecover.Database;
import edu.upenn.cis.citation.Corecover.Lambda_term;
import edu.upenn.cis.citation.Corecover.Query;
import edu.upenn.cis.citation.Corecover.Subgoal;
import edu.upenn.cis.citation.Corecover.Tuple;
import edu.upenn.cis.citation.Operation.Conditions;
import edu.upenn.cis.citation.Operation.Operation;
import edu.upenn.cis.citation.citation_view.Head_strs;
import edu.upenn.cis.citation.init.init;

public class Single_view {
  
  public String token_string = new String ();
  
  public String view_name = new String();
  
  public Vector<Lambda_term> lambda_terms = new Vector<Lambda_term>();
  
  public HashMap<String, String> subgoal_name_mappings = new HashMap<String, String>();
  
  public HashSet<Tuple> view_mappings = new HashSet<Tuple>();
  
  public HashMap<Tuple, Vector<Integer>> view_mapping_view_why_prov_token_col_ids_mapping = new HashMap<Tuple, Vector<Integer>>();
  
  public HashMap<Tuple, Vector<Integer>> view_mapping_q_why_prov_token_col_ids_mapping = new HashMap<Tuple, Vector<Integer>>();
  
  public HashMap<Tuple, Vector<int[][]>> view_mapping_condition_ids_mappings = new HashMap<Tuple, Vector<int[][]>>();
  
  public HashMap<Tuple, Vector<int[]>> view_mapping_lambda_term_ids_mappings = new HashMap<Tuple, Vector<int[]>>();
  
  HashMap<String, Integer> subgoal_name_id_mappings = new HashMap<String, Integer>();

  //subgoals
  public Vector<Subgoal> subgoals = new Vector<Subgoal>();
  
  public Vector<Conditions> conditions = new Vector<Conditions>();;
    
  public Vector<String> operation = new Vector<String>();
  
  //heads
  public Subgoal head;
  
  //mappings between why provenance tokens and lambda variables
  public HashMap<String, Vector<String>> why_prov_lambda_variable_mappings = new HashMap<String, Vector<String>>();
  
  //mappings between subgoal and lambda terms
  public HashMap<String, Vector<Lambda_term>> subgoal_lambda_term_mappings = new HashMap<String, Vector<Lambda_term>>();
  
  //mappings between why provenance tokens and ids in the why_provenance vector
  public HashMap<String, Vector<String[]>> why_prov_ids_mappings = new HashMap<String, Vector<String[]>>();
  
  //vector to store why and where provenances
  public Vector<String []> why_where_provs = new Vector<String []>();
  
  public String token_sequence = new String();
  
  public static String [] numeric_data_type = {"smallint","integer", "bigint", "decimal", "numeric","real","double precision","serial", "bigserial"};
  
  //construction function for conjunctive queries
  
//  void init_condition_ids(Vector<Conditions> conditions, HashMap<String, Integer> subgoal_names)
//  {
//    for(int i = 0; i<conditions.size(); i++)
//    {
//      Conditions condition = conditions.get(i);
//      
//      String subgoal1 = condition.subgoal1;
//      
//      int subgoal_id1 = subgoal_names.get(subgoal1);
//      
//      Argument arg1 = condition.arg1;
//      
//      int arg_id1 = subgoals.get(subgoal_id1).args.indexOf(arg1);
//      
//      Argument arg2 = condition.arg2;
//      
//      int subgoal_id2 = -1;
//      
//      int arg_id2 = -1;
//      
//      if(!arg2.isConst())
//      {
//        String subgoal2 = condition.subgoal2;
//        
//        subgoal_id2 = subgoal_names.get(subgoal2);
//        
//        arg_id2 = subgoals.get(subgoal_id2).args.indexOf(arg2);
//      }
//      
//      int [][]ids = new int[2][2];
//      
//      ids[0][0] = subgoal_id1;
//      
//      ids[0][1] = arg_id1;
//      
//      ids[1][0] = subgoal_id2;
//      
//      ids[1][1] = arg_id2;
//      
//      condition_ids.add(ids);
//      
//    }
//  }
//  
  public Single_view(Query view, String view_name, Connection c, PreparedStatement pst) throws SQLException
  {
    subgoals = view.body;
    
    conditions = view.conditions;
    
    head = view.head;
        
    this.lambda_terms = view.lambda_term;
    
    this.subgoal_name_mappings = view.subgoal_name_mapping;
    
    this.view_name = view_name;
    
    //build mappings between subgoal and lambda terms
    build_subgoal_lambda_term_mappings_conjunctive_query(view);
        
    for(int i = 0; i<subgoals.size();i++)
    {
      subgoal_name_id_mappings.put(subgoals.get(i).name, i);
    }
    
//    init_condition_ids(conditions, subgoal_name_id_mappings);
    
//    String sql = Query_converter.data2sql_with_token_columns(view);
//        
//    pst = c.prepareStatement(sql);
//    
//    ResultSet rs = pst.executeQuery();
//    
//    ResultSetMetaData meta_data = rs.getMetaData();
//    
//    while(rs.next())
//    {
//      store_why_where_prov_tokens(rs, view);
//    }
//    
//    System.out.println(token_sequence);
  }
  
  void store_why_where_prov_tokens(ResultSet rs, Query view) throws SQLException
  {
    
    String [] why_where_prov = new String[view.head.args.size() + view.body.size()];
    
    String curr_token_sequence = "(";
    
    int num = 0;
    
    for(int i = 0; i<view.head.size(); i++)
    {
      
      if(i >= 1)
        curr_token_sequence += ",";
      
      String where_token = rs.getString(i + 1);
      
      why_where_prov[num++] = where_token;
      
      curr_token_sequence += where_token;     
      
    }
    
    for(int i = 0; i<view.body.size(); i++)
    {
      String why_prov_token = rs.getString(i + 1 + view.head.args.size());
      
      if(why_prov_ids_mappings.get(why_prov_token) == null)
      {
        Vector<String[]> ids = new Vector<String[]>();
        
        ids.add(why_where_prov);
        
        why_prov_ids_mappings.put(why_prov_token, ids);
      }
      else
      {
        why_prov_ids_mappings.get(why_prov_token).add(why_where_prov);
      }
      
      why_where_prov[num++] = why_prov_token;
      
      curr_token_sequence += "," + why_prov_token;
    }
    
    why_where_provs.add(why_where_prov);
    
    token_sequence += curr_token_sequence + ")";
  }
  
  void build_subgoal_lambda_term_mappings_conjunctive_query(Query view)
  {
    for(int i = 0; i<view.lambda_term.size(); i++)
    {
      Lambda_term l_term = view.lambda_term.get(i);
      
      String relation_name = l_term.table_name;
      
      String arg_name = l_term.arg_name.substring(l_term.arg_name.indexOf(init.separator) + 1, l_term.arg_name.length());
      
      if(subgoal_lambda_term_mappings.get(relation_name) == null)
      {
        Vector<Lambda_term> arg_names = new Vector<Lambda_term>();
        
        arg_names.add(l_term);
        
        subgoal_lambda_term_mappings.put(relation_name, arg_names);
      }
      else
      {
        subgoal_lambda_term_mappings.get(relation_name).add(l_term);
      }
      
    }
  }
  
  public void build_view_mappings(Vector<Subgoal> subgoals, HashMap<String, String> subgoal_name_mappings)
  {
    Database canDb = CoreCover.constructCanonicalDB(subgoals, subgoal_name_mappings);
    
    view_mappings = CoreCover.computeViewTuples(canDb, this);
    
    for(Iterator iter = view_mappings.iterator(); iter.hasNext();)
    {
      Tuple tuple = (Tuple) iter.next();
      
      Vector<Integer> v_why_col_ids = get_view_why_token_column_ids(tuple);
      
      view_mapping_view_why_prov_token_col_ids_mapping.put(tuple, v_why_col_ids);
      
      Vector<Integer> q_why_col_ids = get_q_why_token_column_ids(tuple, v_why_col_ids, subgoals);
      
      view_mapping_q_why_prov_token_col_ids_mapping.put(tuple, q_why_col_ids);
    }
    
  }
  
  public void build_view_mappings(Vector<Subgoal> subgoals, HashMap<String, String> subgoal_name_mappings, HashSet<String> tables, HashMap<String, Integer> subgoal_id_mappings, HashMap<Tuple, Vector<Integer>> tuple_valid_rows)
  {
    Database canDb = CoreCover.constructCanonicalDB(subgoals, subgoal_name_mappings);
    
    view_mappings = CoreCover.computeViewTuples(canDb, this);
    
    for(Iterator iter = view_mappings.iterator(); iter.hasNext();)
    {
      Tuple tuple = (Tuple) iter.next();
      
      Vector<Integer> row_ids = new Vector<Integer>();
      
      tuple_valid_rows.put(tuple, row_ids);
      
      Vector<Integer> v_why_col_ids = get_view_why_token_column_ids(tuple);
      
      view_mapping_view_why_prov_token_col_ids_mapping.put(tuple, v_why_col_ids);
      
      Vector<Integer> q_why_col_ids = get_q_why_token_column_ids(tuple, v_why_col_ids, subgoals);
      
      view_mapping_q_why_prov_token_col_ids_mapping.put(tuple, q_why_col_ids);
      
      HashMap<Integer, Integer> v_why_col_id_q_why_col_id_mappings = new HashMap<Integer, Integer>();
      
      for(int i = 0; i<v_why_col_ids.size(); i++)
      {
        v_why_col_id_q_why_col_id_mappings.put(q_why_col_ids.get(i), i);
      }
      
      HashSet<String> q_mapped_relations = tuple.get_relations();
      
      for(Iterator iter2 = q_mapped_relations.iterator(); iter2.hasNext();)
      {
        String curr_relation = (String) iter2.next();
        
        String origin_relation_name = subgoal_name_mappings.get(curr_relation);
        
        tables.add(origin_relation_name);
        
      }
      
      Vector<int[][]> ids = new Vector<int[][]>();
      
      for(int i = 0; i<tuple.conditions.size(); i++)
      {
        Conditions condition = tuple.conditions.get(i);
        
        String subgoal1 = condition.subgoal1;
        
        int subgoal_id1 = subgoal_id_mappings.get(subgoal1);
        
        Integer q_subgoal_id1 = v_why_col_id_q_why_col_id_mappings.get(subgoal_id1);
        
        if(q_subgoal_id1 == null)
        {
          q_subgoal_id1 = -1;
        }
                
        Argument arg1 = condition.arg1;
        
        int arg_id1 = subgoals.get(subgoal_id1).args.indexOf(arg1);
        
        Argument arg2 = condition.arg2;
        
        int subgoal_id2 = -1;
        
        int arg_id2 = -1;
        
        Integer q_subgoal_id2 = -1;
        
        if(!arg2.isConst() && condition.get_mapping2)
        {
          String subgoal2 = condition.subgoal2;
          
          subgoal_id2 = subgoal_id_mappings.get(subgoal2);
          
          q_subgoal_id2 = v_why_col_id_q_why_col_id_mappings.get(subgoal_id2);
          
          if(q_subgoal_id2 == null)
          {
            q_subgoal_id2 = -1;
          }
          
          arg_id2 = subgoals.get(subgoal_id2).args.indexOf(arg2);
        }
        
        int [][] curr_ids = new int[2][2];
        
        curr_ids[0][0] = q_subgoal_id1;
        
        curr_ids[0][1] = arg_id1;
        
        curr_ids[1][0] = q_subgoal_id2;
        
        curr_ids[1][1] = arg_id2;
        
        if(q_subgoal_id1 >= 0 || q_subgoal_id2 >= 0)
          ids.add(curr_ids);
      }
      
      Vector<int[]> l_ids = new Vector<int[]>();
      
      for(int i = 0; i<tuple.lambda_terms.size(); i++)
      {
        Lambda_term l_term = tuple.lambda_terms.get(i);
        
        String subgoal1 = l_term.table_name;
        
        int subgoal_id1 = subgoal_id_mappings.get(subgoal1);
        
        Integer q_subgoal_id1 = v_why_col_id_q_why_col_id_mappings.get(subgoal_id1);
        
        Argument arg1 = l_term.arg;
        
        int arg_id1 = subgoals.get(subgoal_id1).args.indexOf(arg1);
        
        int[] curr_ids = new int[2];
        
        curr_ids[0] = q_subgoal_id1;
        
        curr_ids[1] = arg_id1;
        
        l_ids.add(curr_ids);
      }
      
      view_mapping_lambda_term_ids_mappings.put(tuple, l_ids);
      
      view_mapping_condition_ids_mappings.put(tuple, ids);

//      System.out.println(tuple);
    }
    
  }
  
  public Vector<Integer> get_view_why_token_column_ids(Tuple tuple)
  {
    HashMap<String, String> subgoal_mappings = tuple.mapSubgoals_str;
    
    Vector<Integer> ids = new Vector<Integer>();
    
    for(int i = 0; i<subgoals.size(); i++)
    {
      String name = subgoals.get(i).name;
      
      if(subgoal_mappings.get(name)!=null)
      {
        ids.add(i);
      }
      
    }
    
    return ids;
  }
  
  public Vector<Integer> get_q_why_token_column_ids(Tuple tuple, Vector<Integer> v_ids, Vector<Subgoal> q_subgoals)
  {
    Vector<Integer> q_ids = new Vector<Integer>();
    
    for(int i = 0; i<v_ids.size(); i++)
    {
      int v_id = v_ids.get(i);
      
      Subgoal subgoal = this.subgoals.get(v_id);
      
      Subgoal q_subgoal = (Subgoal) tuple.mapSubgoals.get(subgoal);
      
      int q_id = q_subgoals.indexOf(q_subgoal);
      
      q_ids.add(q_id);
      
    }
    
    return q_ids;
  }

  public void check_why_provenance_tokens(HashSet<Tuple> curr_view_mappings, Vector<String> q_why_tokens)
  {
    
    for(Iterator iter = curr_view_mappings.iterator(); iter.hasNext();)
    {
      Tuple tuple = (Tuple) iter.next();
      
      Vector<Integer> q_why_column_ids = view_mapping_q_why_prov_token_col_ids_mapping.get(tuple);
      
      Vector<Integer> v_why_column_ids = view_mapping_view_why_prov_token_col_ids_mapping.get(tuple);
      
      int num = 0;
      
      String token_seq = ".*\\(.*";
      
      for(int i = 0; i<subgoals.size(); i++)
      {
        if(i >= 1)
          token_seq += ",";
        
        if(i == v_why_column_ids.get(num))
        {          
          token_seq += "(" + q_why_tokens.get(q_why_column_ids.get(num)) + ")";
                 
          num++;
        }
        else
        {
          token_seq += ".*";
        }
      }
      
      token_seq += "\\).*";
      
      if(!token_string.matches(token_seq))
        iter.remove();   
      
    }
    
    
  }
  
  public boolean check_provenance_tokens(String q_why_token)
  {      
      if(!token_sequence.matches(q_why_token))
        return false;
      
      return true;
    
    
  }
  
  public boolean check_where_provenance_token(String where_token)
  {
    System.out.println();
    
    System.out.println(token_sequence);
    
    System.out.println(where_token);
    
    System.out.println();
    
    if(!token_sequence.matches(where_token))
      return false;
    
    return true;
    
  }
  
  public void check_where_why_provenance_tokens(HashSet<Tuple> curr_view_mappings, String where_token, Vector<String> q_why_tokens)
  {
    
    for(Iterator iter = curr_view_mappings.iterator(); iter.hasNext();)
    {
      Tuple tuple = (Tuple) iter.next();
      
      Vector<Integer> q_why_column_ids = view_mapping_q_why_prov_token_col_ids_mapping.get(tuple);
      
      Vector<Integer> v_why_column_ids = view_mapping_view_why_prov_token_col_ids_mapping.get(tuple);
      
      int num = 0;
      
      String token_seq = ".*\\(.*("+ where_token +",).*,";
      
      for(int i = 0; i<subgoals.size(); i++)
      {
        if(i >= 1)
          token_seq += ",";
        
        if(i == v_why_column_ids.get(num))
        {          
          token_seq += "(" + q_why_tokens.get(q_why_column_ids.get(num)) + ")";
                 
          num++;
        }
        else
        {
          token_seq += ".*";
        }
      }
      
      token_seq += "\\).*";
      
      if(!token_string.matches(token_seq))
        iter.remove();   
      
    }
    
    
  }
  
  public String get_q_why_provenance_token_seq(Vector<String> q_why_tokens, Tuple tuple)
  {    
    Vector<Integer> q_why_column_ids = view_mapping_q_why_prov_token_col_ids_mapping.get(tuple);
    
    Vector<Integer> v_why_column_ids = view_mapping_view_why_prov_token_col_ids_mapping.get(tuple);
    
    int num = 0;
    
    String why_token_seq = ".*?\\(.*?";
    
    for(int i = 0; i<subgoals.size(); i++)
    {
      if(i >= 1)
        why_token_seq += ",";
      
      if(i == v_why_column_ids.get(num))
      {          
        why_token_seq += "(" + q_why_tokens.get(q_why_column_ids.get(num)) + ")";
               
        num++;
      }
      else
      {
        why_token_seq += ".*?";
      }
    }
    
    why_token_seq += "\\).*?";
    
    return why_token_seq;
    
  }
  
  void evaluate_single_subgoal_args(Head_strs values, Subgoal subgoal)
  {
    Vector<Argument> args = subgoal.args;
    
    for(int i = 0; i<args.size(); i++)
    {
      String value = values.head_vals.get(i);
      
      args.get(i).set_value(value);
    }
  }
  
  public void reset_values()
  {
    for(int i = 0; i<subgoals.size(); i++)
    {
      Vector<Argument> args = subgoals.get(i).args;
      
      for(int j = 0; j<args.size(); j++)
      {
        args.get(j).value = null;
      }
    }
  }
  
  public void evaluate_args(Vector<Head_strs> values, Tuple tuple)
  {
    Vector<Integer> q_why_column_ids = view_mapping_q_why_prov_token_col_ids_mapping.get(tuple);
    
    Vector<Integer> v_why_column_ids = view_mapping_view_why_prov_token_col_ids_mapping.get(tuple);
    
    Vector<int[][]> condition_ids = view_mapping_condition_ids_mappings.get(tuple);
    
    for(int i = 0; i<condition_ids.size();i++)
    {
      int [][]ids = condition_ids.get(i);
      
      int subgoal_id1 = ids[0][0];
      
      int subgoal_id2 = ids[1][0];
      
      if(subgoal_id1 >= 0)
      {
        Head_strs curr_values = values.get(q_why_column_ids.get(subgoal_id1));
        
        Argument arg1 = (Argument) subgoals.get(v_why_column_ids.get(subgoal_id1)).args.get(ids[0][1]);
        
        arg1.set_value(curr_values.head_vals.get(ids[0][1]));
      }
      
      if(subgoal_id2 >= 0)
      {
        Head_strs curr_values = values.get(q_why_column_ids.get(subgoal_id2));
        
        Argument arg2 = (Argument) subgoals.get(v_why_column_ids.get(subgoal_id2)).args.get(ids[1][1]);
        
        arg2.set_value(curr_values.head_vals.get(ids[1][1]));
      }
    }
    
//    for(int i = 0; i<q_why_column_ids.size(); i++)
//    {
//      Head_strs curr_values = values.get(q_why_column_ids.get(i));
//      
//      evaluate_single_subgoal_args(curr_values, subgoals.get(v_why_column_ids.get(i)));
//      
//    }
    

  }
  
  boolean check_condition_satisfiability_fully_evaluated(Argument arg1, Argument arg2, Operation op)
  {
    HashSet<String> data_type_set = new HashSet<String>(Arrays.asList(numeric_data_type));

    if(data_type_set.contains(arg1.data_type) && data_type_set.contains(arg2.data_type))
    {
      double value1 = Double.valueOf(arg1.value);
      
      double value2 = Double.valueOf(arg2.value);
      
      if(value1 == value2 && op.get_op_name().equals("="))
      {
        return true;
      }
      
      if(value1 > value2 && (op.get_op_name().equals(">=") || op.get_op_name().equals(">")))
      {
        return true;
      }
      
      if(value1 < value2 && (op.get_op_name().equals("<=") || op.get_op_name().equals("<")))
      {
        return true;
      }
      
      if(value1 != value2 && (op.get_op_name().equals("<>")))
      {
        return true;
      }
      
      return false;
    }
    else
    {
      String value1 = arg1.value;
      
      String value2 = arg2.value;
      
      if(value1.equals(value2) && op.get_op_name().equals("="))
      {
        return true;
      }
      
      if(value1.compareTo(value2) > 0 && (op.get_op_name().equals(">=") || op.get_op_name().equals(">")))
      {
        return true;
      }
      
      if(value1.compareTo(value2) < 0 && (op.get_op_name().equals("<=") || op.get_op_name().equals("<")))
      {
        return true;
      }
      
      if(!value1.equals(value2) && (op.get_op_name().equals("<>")))
      {
        return true;
      }
      
      return false;
    }
  }
  
  public boolean check_condition_satisfiability(Conditions condition, HashMap<String, Vector<Conditions>> undertermined_conditions)
  {
    Argument arg1 = condition.arg1;
    
    Argument arg2 = condition.arg2;
    
    if((arg1.value != null && arg2.value != null))
    {
        return check_condition_satisfiability_fully_evaluated(condition.arg1, condition.arg2, condition.op);    
    }
    
    
    if(arg1.value != null && arg2.value == null)
    {
//      Argument new_arg = new Argument(arg1.value);
//      
//      new_arg.data_type = arg1.data_type;
//      
//      new_arg.value = arg1.value;
//      
//      Conditions new_condition = new Conditions(arg2, condition.subgoal2, condition.op, new_arg, condition.subgoal1);
//      
      condition.swap_args();
      
      if(undertermined_conditions.get(condition.subgoal1) == null)
      {
        Vector<Conditions> conditions = new Vector<Conditions>();
        
        conditions.add(condition);
        
        undertermined_conditions.put(condition.subgoal1, conditions);
      }
      else
      {
        undertermined_conditions.get(condition.subgoal1).add(condition);
      }
      
      return true;
    }
    
    
    if(arg1.value == null && arg2.value != null)
    {
      
//      Argument new_arg = new Argument(arg2.value);
//      
//      new_arg.data_type = arg2.data_type;
//      
//      new_arg.value = arg2.value;
//      
//      Conditions new_condition = new Conditions(arg1, condition.subgoal1, condition.op, new_arg, condition.subgoal2);
      
      if(undertermined_conditions.get(condition.subgoal1) == null)
      {
        Vector<Conditions> conditions = new Vector<Conditions>();
        
        conditions.add(condition);
        
        undertermined_conditions.put(condition.subgoal1, conditions);
      }
      else
      {
        undertermined_conditions.get(condition.subgoal1).add(condition);
      }
      
      return true;
    }
    
    
    if(arg1.value == null && arg2.value == null)
      return true;
    
    return true;
  }
  
  public void reset_values(Tuple tuple)
  {
    Vector<int[][]> ids = view_mapping_condition_ids_mappings.get(tuple);
    
    Vector<Integer> v_why_column_ids = view_mapping_view_why_prov_token_col_ids_mapping.get(tuple);
    
    for(int i = 0; i<ids.size(); i++)
    {
      int [][] curr_ids = ids.get(i);
      
      int subgoal_id1 = curr_ids[0][0];
      
      int subgoal_id2 = curr_ids[1][0];
      
      if(subgoal_id1 >= 0)
      {
        Argument arg1 = (Argument) subgoals.get(v_why_column_ids.get(subgoal_id1)).args.get(curr_ids[0][1]);
        
        arg1.set_value(null);
      }
      
      if(subgoal_id2 >= 0)
      {
        Argument arg2 = (Argument) subgoals.get(v_why_column_ids.get(subgoal_id2)).args.get(curr_ids[1][1]);
        
        arg2.set_value(null);
      }
    }
    
  }
  
  void input_relation_attr(HashMap<String, HashMap<String, Vector<Integer>>> relation_attr_value_mappings, HashMap<String, Vector<String>> all_values, String relation_name, String attribute, Connection c, PreparedStatement pst) throws SQLException
  {
    String sql = "select " + attribute + " from " + relation_name;
    
    pst= c.prepareStatement(sql);
    
    ResultSet rs = pst.executeQuery();
    
    HashMap<String, Vector<Integer>> value_id_mappings = new HashMap<String, Vector<Integer>>();
    
    int rid = 0;
    
    Vector<String> values = new Vector<String>();
    
    while(rs.next())
    {
      String value = (String) rs.getString(1);
      
      value = "'" + value + "'";
      
      if(value_id_mappings.get(value) == null)
      {
        Vector<Integer> rids = new Vector<Integer>();
        
        rids.add(rid);
        
        value_id_mappings.put(value, rids);
      }
      else
      {
        value_id_mappings.get(value).add(rid);
      }
      
      rid++;
      
      values.add(value);
    }
    
    sql = "select data_type from information_schema.columns where table_name = '" + relation_name + "' and column_name = '" + attribute + "'";
    
    pst = c.prepareStatement(sql);
    
    rs = pst.executeQuery();
    
    String data_type = new String();
    
    if(rs.next())
    {
      data_type = rs.getString(1);
    }
    
    values.add(data_type);
    
    all_values.put(relation_name + init.separator + attribute, values);
  }
  
  String gen_sql_partial_evaluated_condition(String origin_subgoal_name, Vector<Conditions> conditions)
  {
    String sql = "select exists (select * from " + origin_subgoal_name + " where ";
    
    for(int i = 0; i<conditions.size(); i++)
    {
      Conditions condition = conditions.get(i);
      
      if(i >= 1)
        sql += " and ";
      
      sql += condition.arg1.name.substring(condition.arg1.name.indexOf(init.separator) + 1, condition.arg1.name.length());
      
      sql += condition.op;
      
      sql += condition.arg2.value;
    }
    
    sql += ")";
    
    return sql;
  }
  
  public boolean check_undertermined_conditions(HashMap<String, HashMap<String, Vector<Integer>>> rel_attr_value_mappings, HashMap<String, Vector<String>> all_values, HashMap<String, Vector<Conditions>> undetermined_conditions, Connection c, PreparedStatement pst) throws SQLException
  {
    Set<String> tables = undetermined_conditions.keySet();
    
    for(Iterator iter = tables.iterator(); iter.hasNext();)
    {
      String subgoal_name = (String) iter.next();
      
      String origin_subgoal_name = subgoal_name_mappings.get(subgoal_name);
      
      Vector<Conditions> conditions = undetermined_conditions.get(origin_subgoal_name);
     
      String sql = gen_sql_partial_evaluated_condition(origin_subgoal_name, conditions);
      
      pst = c.prepareStatement(sql);
      
      ResultSet rs = pst.executeQuery();
      
      boolean b = false;
      
      if(rs.next())
        b = rs.getBoolean(1);
      
      if(!b)
        return false;
      
//      Vector<Integer> rids = null;
//      
//      for(int i = 0; i<conditions.size(); i++)
//      {
//        Conditions condition = conditions.get(i);
//        
//        Argument arg1 = condition.arg1;
//        
//        String arg_name = arg1.name.substring(arg1.name.indexOf(init.separator) + 1, arg1.name.length());
//        
//        HashMap<String, Vector<Integer>> value_rid_mappings = rel_attr_value_mappings.get(origin_subgoal_name + init.separator + arg_name); 
//        
//        if(value_rid_mappings == null)
//        {
//          input_relation_attr(rel_attr_value_mappings, all_values, origin_subgoal_name, arg_name, c, pst);
//        }
//        
//        if(condition.op.get_op_name().equals("="))
//        {
//          Vector<Integer> curr_rids = value_rid_mappings.get(condition.arg2.name);
//          
//          if(curr_rids == null)
//          {
//            return false;
//          }
//          else
//          {
//            if(rids == null)
//            {
//              rids = curr_rids;
//            }
//            else
//            {
//              rids.retainAll(curr_rids);
//              
//              if(rids.isEmpty())
//                return false;
//            }
//          }
//        }
//        
//      }
//      
//      for(int i = 0; i<conditions.size(); i++)
//      {
//        Conditions condition = conditions.get(i);
//        
//        Argument arg1 = condition.arg1;
//        
//        String arg_name = arg1.name.substring(arg1.name.indexOf(init.separator) + 1, arg1.name.length()); 
//                 
//        Vector<String> curr_values = all_values.get(origin_subgoal_name + init.separator + arg_name);
//        
//        arg1.data_type = curr_values.get(curr_values.size() - 1);
//        
//        for(int j = 0; j<rids.size(); j++)
//        {
//          int curr_rid = rids.get(j);       
//          
//          arg1.value = curr_values.get(curr_rid);
//          
//          if(!check_condition_satisfiability_fully_evaluated(condition.arg1, condition.arg2, condition.op))
//          {
//            rids.removeElementAt(j);
//            
//            j--;
//          }
//        }
//        
//        if(rids.isEmpty())
//          return false;
//      }
    }
    
    return true;
  }
  
  public boolean check_validity(Tuple tuple, HashMap<String, HashMap<String, Vector<Integer>>> rel_attr_value_mappings, Connection c, PreparedStatement pst) throws SQLException
  {
    
    boolean satisfiable = true; 
    
    HashMap<String, Vector<Conditions>> undetermined_conditions = new HashMap<String, Vector<Conditions>>();
    
    for(int i = 0; i<conditions.size(); i++)
    {
      Conditions condition = conditions.get(i);
      
      if(!check_condition_satisfiability(condition, undetermined_conditions))
      {
        satisfiable = false;
        
        break;
      }
      
    }
    
    HashMap<String, Vector<String>> all_values = new HashMap<String, Vector<String>>();
    
    
    if(!check_undertermined_conditions(rel_attr_value_mappings, all_values , undetermined_conditions, c, pst))
      return false;
    
    reset_values(tuple);
    
    return satisfiable;
  }
  
  public HashSet<Head_strs> get_lambda_values(Tuple tuple, ArrayList<Vector<Head_strs>> why_tokens, Vector<Integer> valid_row_ids)
  {
    Vector<int[]> ids = view_mapping_lambda_term_ids_mappings.get(tuple);
    
//    Vector<Integer> v_subgoal_ids = view_mapping_view_why_prov_token_col_ids_mapping.get(tuple);
    
    Vector<Integer> q_subgoal_ids = view_mapping_q_why_prov_token_col_ids_mapping.get(tuple);
    
    HashSet<Head_strs> lambda_values = new HashSet<Head_strs>();
    
    for(int i = 0; i<valid_row_ids.size(); i++)
    {
      Vector<Head_strs> curr_why_tokens = why_tokens.get(valid_row_ids.get(i));
      
      Vector<String> curr_l_values = new Vector<String>();
      
      for(int j = 0; j<ids.size(); j++)
      {
        int [] curr_ids = ids.get(j);
        
//        Subgoal subgoal = subgoals.get(v_subgoal_ids.get(curr_ids[0]));
//        
//        Argument arg = (Argument) subgoal.args.get(curr_ids[1]);
        
        Head_strs why_token = curr_why_tokens.get(q_subgoal_ids.get(curr_ids[0]));
        
        String value = why_token.head_vals.get(curr_ids[1]);
        
//        arg.set_value(value);
        
        curr_l_values.add(value);
        
      }
      
      Head_strs curr_lambda_values = new Head_strs(curr_l_values);
      
      lambda_values.add(curr_lambda_values);
    }
    
    return lambda_values;
    
  }
  
//  public String check_valid_view_mappings(Vector<String> Head_str, Tuple tuple)
//  {    
//    Vector<Integer> q_why_column_ids = view_mapping_q_why_prov_token_col_ids_mapping.get(tuple);
//    
//    Vector<Integer> v_why_column_ids = view_mapping_view_why_prov_token_col_ids_mapping.get(tuple);
//    
//    int num = 0;
//    
//    String why_token_seq = ".*?\\(.*?";
//    
//    for(int i = 0; i<subgoals.size(); i++)
//    {
//      if(i >= 1)
//        why_token_seq += ",";
//      
//      if(i == v_why_column_ids.get(num))
//      {          
//        why_token_seq += "(" + q_why_tokens.get(q_why_column_ids.get(num)) + ")";
//               
//        num++;
//      }
//      else
//      {
//        why_token_seq += ".*?";
//      }
//    }
//    
//    why_token_seq += "\\).*?";
//    
//    return why_token_seq;
//    
//  }
  
  public String get_q_where_why_provenance_token_seq(String q_where_token, Vector<String> q_why_tokens, Tuple tuple)
  {    
    Vector<Integer> q_why_column_ids = view_mapping_q_why_prov_token_col_ids_mapping.get(tuple);
    
    Vector<Integer> v_why_column_ids = view_mapping_view_why_prov_token_col_ids_mapping.get(tuple);
    
    int num = 0;
    
    String why_token_seq = ".*?\\(.*?(" + q_where_token + "),.*?";
    
    for(int i = 0; i<subgoals.size(); i++)
    {
      if(i >= 1)
        why_token_seq += ",";
      
      if(i == v_why_column_ids.get(num))
      {          
        why_token_seq += "(" + q_why_tokens.get(q_why_column_ids.get(num)) + ")";
               
        num++;
      }
      else
      {
        why_token_seq += ".*?";
      }
    }
    
    why_token_seq += "\\).*?";
    
    return why_token_seq;
    
  }
  
  public HashMap<String, Integer> get_citation_queries(Connection c, PreparedStatement pst) throws SQLException
  {
    String sql = "select citation2query.citation_block, citation2query.query_id from view_table join citation2view on (view_table.view = citation2view.view) join citation2query on (citation2query.citation_view_id = citation2view.citation_view_id) where view_table.name = '" + view_name + "'";

    pst = c.prepareStatement(sql);
    
    ResultSet rs = pst.executeQuery();
    
    HashMap<String, Integer> citation_query_ids = new HashMap<String, Integer>();
    
    while(rs.next())
    {
      String block_name = rs.getString(1);
      
     int query_id = rs.getInt(2);
     
     citation_query_ids.put(block_name, query_id);
    }
    
    return citation_query_ids;
  }
  
  @Override
  public String toString()
  {
    return this.view_name + init.separator + subgoal_name_mappings;
  }
  
  public Vector<Subgoal> getBody()
  {
    return subgoals;
  }

  public Subgoal getHead() {
    // TODO Auto-generated method stub
    return head;
  }

  public String getName() {
    // TODO Auto-generated method stub
    return view_name;
  }

  public boolean isDistVar(Argument arg) {
    // TODO Auto-generated method stub
    if (arg.isConst()) return true;
    return  head.getArgs().contains(arg);
  }

  public int getCount() {
    // TODO Auto-generated method stub
    return view_mappings.size();
  }

  public Vector getDistVars() {
    // TODO Auto-generated method stub
    return head.args;
  }
  
  public Vector getUsefulArgs(int index) {
    Vector usefulArgs = (Vector) head.getArgs().clone();
    for (int i = index + 1; i < subgoals.size(); i ++) {
      Subgoal subgoal = subgoals.elementAt(i);

      Vector subgoalArgs = subgoal.getArgs();
      for (int j = 0; j < subgoalArgs.size(); j ++) {
    Argument arg = (Argument) subgoalArgs.elementAt(j);
    if (!arg.isConst() && !usefulArgs.contains(arg)) // no constant
      usefulArgs.add(arg);
      }
    }

    return usefulArgs;
  }

}
package edu.upenn.cis.citation.views;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import edu.upenn.cis.citation.Corecover.Argument;
import edu.upenn.cis.citation.Corecover.Subgoal;
import edu.upenn.cis.citation.citation_view1.Head_strs;
import edu.upenn.cis.citation.query.Query_provenance;

public class Virtual_view_query_generator {
  
  static String gen_query_for_retrieving_materialized_for_grouping_vars(Single_view view, int[] head_var_ids, Vector<Integer> provenance_table_ids, String separater)
  {
    String string = new String();
    
//    for(int i = 0; i<head_var_ids.length; i++)
    for(int i = 0; i<view.head.args.size(); i++)
    {
      Argument arg = (Argument) view.head.args.get(i);
      
      if(i >= 1)
        string += ",";
      
      string += arg.relation_name + separater + arg.attribute_name;
      
    }
    
    return string;
  }
  
  static String gen_query_for_retrieving_materialized_views_for_provenance_attrs(Single_view view, int[] head_var_ids, Vector<Integer> provenance_table_ids)
  {
    String string = new String();
    
    int num = 0;
    
    for(int i = 0; i<provenance_table_ids.size(); i++)
    {
      Subgoal subgoal = view.subgoals.get(provenance_table_ids.get(i));
      
      Vector<Integer> attr_ids = Single_view.relation_primary_key_mappings.get(view.subgoal_name_mappings.get(subgoal.name));
      
      for(int j = 0; j<attr_ids.size(); j++)
      {
        if(num >= 1)
          string += ",";
        
        Argument arg = (Argument) subgoal.args.get(attr_ids.get(j));
        
        string += subgoal.name + "_" + arg.attribute_name;
        
        num++;
      }
    }
    
    return string;
  }
  
  static String gen_query_for_retrieving_materialized_views_for_provenance_attrs2(Single_view view, int[] head_var_ids, Vector<Integer> provenance_table_ids)
  {
    String string = "array_agg(";
    
    int num = 0;
    
    for(int i = 0; i<provenance_table_ids.size(); i++)
    {
      Subgoal subgoal = view.subgoals.get(provenance_table_ids.get(i));
      
      Vector<Integer> attr_ids = Single_view.relation_primary_key_mappings.get(view.subgoal_name_mappings.get(subgoal.name));
      
      for(int j = 0; j<attr_ids.size(); j++)
      {
        if(num >= 1)
          string += "||'" + Query_provenance.separator_input + "'||";
        
        Argument arg = (Argument) subgoal.args.get(attr_ids.get(j));
        
        string += "cast (" + subgoal.name + "." + arg.attribute_name + " as text)";
        
        num++;
      }
    }
    
    
    string += ")";
    
    return string;
  }
  
  public static String gen_query_for_retrieving_materialized_views(Single_view view, int[] head_var_ids, Vector<Integer> provenance_table_ids, String grouping_value_arg_expression, String grouping_value_expression)
  {
    String sel_item = gen_query_for_retrieving_materialized_for_grouping_vars(view, head_var_ids, provenance_table_ids, "_");
    
    
    
    if(sel_item.isEmpty())
    {
      sel_item += gen_query_for_retrieving_materialized_views_for_provenance_attrs(view, head_var_ids, provenance_table_ids);
    }
    else
    {
      sel_item += "," + gen_query_for_retrieving_materialized_views_for_provenance_attrs(view, head_var_ids, provenance_table_ids);
    }
    
    String query = "select " + sel_item + " from " + view.view_name + " where " + grouping_value_arg_expression + "=ANY(VALUES" + grouping_value_expression + ")";
    
    return materilization(view) + " " + query;
  }
  
  
  public static String gen_query_for_retrieving_materialized_views2(Single_view view, int[] head_var_ids, Vector<Integer> provenance_table_ids, String grouping_value_arg_expression, String grouping_value_expression)
  {
    String sel_item = gen_query_for_retrieving_materialized_for_grouping_vars(view, head_var_ids, provenance_table_ids, ".");
    
    String all_sel_item = sel_item;
    
    if(sel_item.isEmpty())
    {
      all_sel_item += gen_query_for_retrieving_materialized_views_for_provenance_attrs2(view, head_var_ids, provenance_table_ids);
    }
    else
    {
      all_sel_item += "," + gen_query_for_retrieving_materialized_views_for_provenance_attrs2(view, head_var_ids, provenance_table_ids);
    }
    
    String query = "select " + all_sel_item + " from ";
    
    HashMap<String, String> subgoal_name_mappings = view.subgoal_name_mappings;
    
    for(int i = 0; i<view.subgoals.size(); i++)
    {
      if(i >= 1)
        query += ",";
      
      String subgoal_name = view.subgoals.get(i).name; 
      
      query += subgoal_name_mappings.get(subgoal_name) + " " + subgoal_name;
    }
    
    String condition = Query_converter.get_condition(view, false);
    
    if(!condition.isEmpty())
    {
      query += " where " + condition;// + " and " + grouping_value_arg_expression + "=ANY(VALUES" + grouping_value_expression + ")";
    }
//    else
//    {
//      query += " where " + grouping_value_arg_expression + "=ANY(VALUES" + grouping_value_expression + ")";
//    }
    
//    view.view_name + " where " + 
    
    if(!sel_item.isEmpty())
    {
      query += " group by " + sel_item;  
    }
    
    String having_clause = Query_converter.get_having_clauses(view, false);
    
    if(!having_clause.isEmpty())
      query += " having " + having_clause;
    
//    if(query.length() > 10000)
//      System.out.println(query.substring(0, 10000));
//    else
//      System.out.println(query);
//    
//    System.out.println(query.substring(query.length() - 1000, query.length()));
    
    return query;
  }
  
  
  public static String materilization(Single_view view)
  {
    Vector<String> indexed_cols = new Vector<String>();
    
    Vector<String> grouping_attrs_strings = new Vector<String>();
    
    Vector<String> provenance_attrs_strings = new Vector<String>();
    
    String sql = Query_converter.datalog2sql_view_conjunction_virtual(view, indexed_cols, grouping_attrs_strings, provenance_attrs_strings);
    
    return sql;
    
//    build_index(provenance_attrs_strings, view.view_name, c, pst);
//    build_index_on_materilized_views_for_grouping_attributes(view, c, pst);
//    
  }
  
//  static void get_query_grouping_values(HashSet<Head_strs> head_values)
//  {
//    for(Head_strs head_val : head_values)
//    {
//      ArrayList<Integer> curr_rids = tuple_why_prov_mappings.get(head_val);
//      
////      System.out.println("rids::" + curr_rids);
//      
//      for(Integer rid: curr_rids)
//      {
//        
//        
////        grouping_values.add(view.evaluate_view_grouping_attrs2(values_from_why_tokens.get(rid), tuple, query));
//        String value = view.evaluate_view_grouping_attrs(values_from_why_tokens.get(rid), tuple, query, relation_seqs); 
//        if(!grouping_values.contains(value))
//        {
//          if(count >= 1)
//            sb.append(",");
//          sb.append(value);
//          grouping_values.add(value);
//          count ++;
//        }
//        
//        
////        System.out.println(grouping_values.length());
////        grouping_value_condition_string += curr_grouping_value_condition_string;
////        view_grouping_values_from_query.add(view_grouping_values);
////        System.out.println(count);
//        
//      }
//      
//    }
//  }

}

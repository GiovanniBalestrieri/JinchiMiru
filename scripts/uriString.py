chair = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#Chair"
coke = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#Coke"
table = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#Table"
beer = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#Beer"

lexicalRef = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#lexicalReference"

altRefPred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#hasAlternativeReference"

prefRefPred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#hasPreferredReference"

positionPred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#hasPosition"

coordXPred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#float_coordinates_x"

coordYPred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#float_coordinates_y"

coordZPred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#float_coordinates_z"

sizePred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#hasSize"

sizeXPred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#float_size_x"

sizeYPred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#float_size_y"

sizeZPred = "http://www.semanticweb.org/ontologies/2016/1/semantic_mapping_domain_model#float_size_z"


ALL = "all"
TERMINATOR = "T";
HELP = "help";
SEPARATOR = "\t";
HEADER = "a";
INIT_HEADER = "i";
UPDATE_HEADER = "u";
EXPORT_HEADER = "e";
EXPORT_EXT_HEADER = "export";
LOAD_ACK_HEADER = "loadack"
LOAD_HEADER = "load"
ADD_HEADER = "a";
DEL_HEADER = "del";
LIST_HEADER = "g";
ADD_BY_REF_HEADER = "add";
LIST_REF_HEADER = "list";
ADD_ACK_HEADER = "addack";
GET_ACK_HEADER = "getack";
DEL_ACK_HEADER = "delack";
EXPORT_ACK_HEADER = "exportack";
LIST_ACK_HEADER = "listack";
LIST_OBJ_ACK_HEADER = "listObjectack";
DEL_OBJ_ACK_HEADER = "delObjectack";
ADD_RQ_HEADER = "add"
COORDINATOR = " [ Coordinator Node ] "
EXTERN = " [ Extern Node ] "

# Default settings Add method
uri_to_add = "new_chair"
class_to_add = "Chair"
defaultConfig = "default"
pose_to_add_x = "-4"
pose_to_add_y = "4"
pose_to_add_z = "0.10"
pose_to_add =  pose_to_add_x + ","+pose_to_add_y +","+pose_to_add_z
lex_ref_to_add = "sediola"


# Json options
JSON_MIN = True
TAG = "entities"
TAG_ATOM = "atom"
TAG_ATOM_FULL = "atom_full"
TAG_TYPE = "type"
TAG_TYPE_FULL = "type_full"
TAG_COORD = "coordinate"
TAG_COORD_FULL = "coordinates"
TAG_LEX = "preferredLexicalReference"
TAG_COORD_X = "x"
TAG_COORD_Y = "y"
TAG_COORD_Z = "z"


# !<sup>norm</sup>[](https://github.com/narcolepticsnowman/BabyORM/blob/master/penguin_logo_small.png?raw=true) NanoORM 
A simplified ORM for accomplishing simple tasks.
 
### How the F*** do I use this thing?
###### Required to work
2. The Entity/DTO type MUST have a ```@PK``` on exactly 1 Field. No multi value keys, for now.
1. Get a new Repo 
   - ``` new BabyRepo<Foo>(){};```<sup>*</sup> or ```BabyRepo.forType(Foo.class)); ```
1. Set a connection supplier (i.e. ConnectionPoolX::getConnection)

<sup>\*Th ```{}``` are necessary to infer the class at runtime</sup>

###### Fancy bits
If the names of your classes and fields do not match exactly to the names in the database, you will
need to provide the appropriate name annotation.




### About
This ORM is meant to provide super light weight ORM functionality without any dependencies.
It provides full CRUD capabilities for any entity object. The plan is to add only the most commonly needed features to keep usage simple.

##### Key features:
    - Easy to learn and use
    - Automagically map object fields to row columns and vice versa
    - Insert, Update, Delete records
    - Query by any set of columns, either ANDed or ORed together
    - Arbitrary SQL query execution to fetch objects (think views in code).
    
    

##### planned features:
    - Support storing regular object types as JSON

### Why build another ORM?
Other ORMs are over kill when you need to accomplish simple row to object mapping.
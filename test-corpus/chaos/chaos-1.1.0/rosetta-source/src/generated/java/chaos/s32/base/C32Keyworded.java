package chaos.s32.base;

import chaos.s32.base.meta.C32KeywordedMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Attributes named after Java keywords at POJO seats (the func-java-keyword-attr class widened).
 * @version 1.0.0
 */
@RosettaDataType(value="C32Keyworded", builder=C32Keyworded.C32KeywordedBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C32Keyworded", model="chaos", builder=C32Keyworded.C32KeywordedBuilderImpl.class, version="1.0.0")
public interface C32Keyworded extends RosettaModelObject {

	C32KeywordedMeta metaData = new C32KeywordedMeta();

	/*********************** Getter Methods  ***********************/
	String _getClass();
	BigDecimal getLong();
	String getChar();
	String getThis();
	List<String> getNew();
	String getPrivate();
	BigDecimal getReturn();
	String getPackage();
	String getNull();
	List<String> getVar();
	Boolean getAbstract();
	String getThrows();
	C32Aux getAux();

	/*********************** Build Methods  ***********************/
	C32Keyworded build();
	
	C32Keyworded.C32KeywordedBuilder toBuilder();
	
	static C32Keyworded.C32KeywordedBuilder builder() {
		return new C32Keyworded.C32KeywordedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C32Keyworded> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C32Keyworded> getType() {
		return C32Keyworded.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("class"), String.class, _getClass(), this);
		processor.processBasic(path.newSubPath("long"), BigDecimal.class, getLong(), this);
		processor.processBasic(path.newSubPath("char"), String.class, getChar(), this);
		processor.processBasic(path.newSubPath("this"), String.class, getThis(), this);
		processor.processBasic(path.newSubPath("new"), String.class, getNew(), this);
		processor.processBasic(path.newSubPath("private"), String.class, getPrivate(), this);
		processor.processBasic(path.newSubPath("return"), BigDecimal.class, getReturn(), this);
		processor.processBasic(path.newSubPath("package"), String.class, getPackage(), this);
		processor.processBasic(path.newSubPath("null"), String.class, getNull(), this);
		processor.processBasic(path.newSubPath("var"), String.class, getVar(), this);
		processor.processBasic(path.newSubPath("abstract"), Boolean.class, getAbstract(), this);
		processor.processBasic(path.newSubPath("throws"), String.class, getThrows(), this);
		processRosetta(path.newSubPath("aux"), processor, C32Aux.class, getAux());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C32KeywordedBuilder extends C32Keyworded, RosettaModelObjectBuilder {
		C32Aux.C32AuxBuilder getOrCreateAux();
		@Override
		C32Aux.C32AuxBuilder getAux();
		C32Keyworded.C32KeywordedBuilder setClass(String _class);
		C32Keyworded.C32KeywordedBuilder setLong(BigDecimal _long);
		C32Keyworded.C32KeywordedBuilder setChar(String _char);
		C32Keyworded.C32KeywordedBuilder setThis(String _this);
		C32Keyworded.C32KeywordedBuilder addNew(String _new);
		C32Keyworded.C32KeywordedBuilder addNew(String _new, int idx);
		C32Keyworded.C32KeywordedBuilder addNew(List<String> _new);
		C32Keyworded.C32KeywordedBuilder setNew(List<String> _new);
		C32Keyworded.C32KeywordedBuilder setPrivate(String _private);
		C32Keyworded.C32KeywordedBuilder setReturn(BigDecimal _return);
		C32Keyworded.C32KeywordedBuilder setPackage(String _package);
		C32Keyworded.C32KeywordedBuilder setNull(String _null);
		C32Keyworded.C32KeywordedBuilder addVar(String var);
		C32Keyworded.C32KeywordedBuilder addVar(String var, int idx);
		C32Keyworded.C32KeywordedBuilder addVar(List<String> var);
		C32Keyworded.C32KeywordedBuilder setVar(List<String> var);
		C32Keyworded.C32KeywordedBuilder setAbstract(Boolean _abstract);
		C32Keyworded.C32KeywordedBuilder setThrows(String _throws);
		C32Keyworded.C32KeywordedBuilder setAux(C32Aux aux);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("class"), String.class, _getClass(), this);
			processor.processBasic(path.newSubPath("long"), BigDecimal.class, getLong(), this);
			processor.processBasic(path.newSubPath("char"), String.class, getChar(), this);
			processor.processBasic(path.newSubPath("this"), String.class, getThis(), this);
			processor.processBasic(path.newSubPath("new"), String.class, getNew(), this);
			processor.processBasic(path.newSubPath("private"), String.class, getPrivate(), this);
			processor.processBasic(path.newSubPath("return"), BigDecimal.class, getReturn(), this);
			processor.processBasic(path.newSubPath("package"), String.class, getPackage(), this);
			processor.processBasic(path.newSubPath("null"), String.class, getNull(), this);
			processor.processBasic(path.newSubPath("var"), String.class, getVar(), this);
			processor.processBasic(path.newSubPath("abstract"), Boolean.class, getAbstract(), this);
			processor.processBasic(path.newSubPath("throws"), String.class, getThrows(), this);
			processRosetta(path.newSubPath("aux"), processor, C32Aux.C32AuxBuilder.class, getAux());
		}
		

		C32Keyworded.C32KeywordedBuilder prune();
	}

	/*********************** Immutable Implementation of C32Keyworded  ***********************/
	class C32KeywordedImpl implements C32Keyworded {
		private final String _class;
		private final BigDecimal _long;
		private final String _char;
		private final String _this;
		private final List<String> _new;
		private final String _private;
		private final BigDecimal _return;
		private final String _package;
		private final String _null;
		private final List<String> var;
		private final Boolean _abstract;
		private final String _throws;
		private final C32Aux aux;
		
		protected C32KeywordedImpl(C32Keyworded.C32KeywordedBuilder builder) {
			this._class = builder._getClass();
			this._long = builder.getLong();
			this._char = builder.getChar();
			this._this = builder.getThis();
			this._new = ofNullable(builder.getNew()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this._private = builder.getPrivate();
			this._return = builder.getReturn();
			this._package = builder.getPackage();
			this._null = builder.getNull();
			this.var = ofNullable(builder.getVar()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this._abstract = builder.getAbstract();
			this._throws = builder.getThrows();
			this.aux = ofNullable(builder.getAux()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("class")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("class")
		public String _getClass() {
			return _class;
		}
		
		@Override
		@RosettaAttribute("long")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("long")
		public BigDecimal getLong() {
			return _long;
		}
		
		@Override
		@RosettaAttribute("char")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("char")
		public String getChar() {
			return _char;
		}
		
		@Override
		@RosettaAttribute("this")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("this")
		public String getThis() {
			return _this;
		}
		
		@Override
		@RosettaAttribute("new")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("new")
		public List<String> getNew() {
			return _new;
		}
		
		@Override
		@RosettaAttribute("private")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("private")
		public String getPrivate() {
			return _private;
		}
		
		@Override
		@RosettaAttribute("return")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("return")
		public BigDecimal getReturn() {
			return _return;
		}
		
		@Override
		@RosettaAttribute("package")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("package")
		public String getPackage() {
			return _package;
		}
		
		@Override
		@RosettaAttribute("null")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("null")
		public String getNull() {
			return _null;
		}
		
		@Override
		@RosettaAttribute("var")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("var")
		public List<String> getVar() {
			return var;
		}
		
		@Override
		@RosettaAttribute("abstract")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("abstract")
		public Boolean getAbstract() {
			return _abstract;
		}
		
		@Override
		@RosettaAttribute("throws")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("throws")
		public String getThrows() {
			return _throws;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C32Aux getAux() {
			return aux;
		}
		
		@Override
		public C32Keyworded build() {
			return this;
		}
		
		@Override
		public C32Keyworded.C32KeywordedBuilder toBuilder() {
			C32Keyworded.C32KeywordedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C32Keyworded.C32KeywordedBuilder builder) {
			ofNullable(_getClass()).ifPresent(builder::setClass);
			ofNullable(getLong()).ifPresent(builder::setLong);
			ofNullable(getChar()).ifPresent(builder::setChar);
			ofNullable(getThis()).ifPresent(builder::setThis);
			ofNullable(getNew()).ifPresent(builder::setNew);
			ofNullable(getPrivate()).ifPresent(builder::setPrivate);
			ofNullable(getReturn()).ifPresent(builder::setReturn);
			ofNullable(getPackage()).ifPresent(builder::setPackage);
			ofNullable(getNull()).ifPresent(builder::setNull);
			ofNullable(getVar()).ifPresent(builder::setVar);
			ofNullable(getAbstract()).ifPresent(builder::setAbstract);
			ofNullable(getThrows()).ifPresent(builder::setThrows);
			ofNullable(getAux()).ifPresent(builder::setAux);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C32Keyworded _that = getType().cast(o);
		
			if (!Objects.equals(_class, _that._getClass())) return false;
			if (!Objects.equals(_long, _that.getLong())) return false;
			if (!Objects.equals(_char, _that.getChar())) return false;
			if (!Objects.equals(_this, _that.getThis())) return false;
			if (!ListEquals.listEquals(_new, _that.getNew())) return false;
			if (!Objects.equals(_private, _that.getPrivate())) return false;
			if (!Objects.equals(_return, _that.getReturn())) return false;
			if (!Objects.equals(_package, _that.getPackage())) return false;
			if (!Objects.equals(_null, _that.getNull())) return false;
			if (!ListEquals.listEquals(var, _that.getVar())) return false;
			if (!Objects.equals(_abstract, _that.getAbstract())) return false;
			if (!Objects.equals(_throws, _that.getThrows())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (_class != null ? _class.hashCode() : 0);
			_result = 31 * _result + (_long != null ? _long.hashCode() : 0);
			_result = 31 * _result + (_char != null ? _char.hashCode() : 0);
			_result = 31 * _result + (_this != null ? _this.hashCode() : 0);
			_result = 31 * _result + (_new != null ? _new.hashCode() : 0);
			_result = 31 * _result + (_private != null ? _private.hashCode() : 0);
			_result = 31 * _result + (_return != null ? _return.hashCode() : 0);
			_result = 31 * _result + (_package != null ? _package.hashCode() : 0);
			_result = 31 * _result + (_null != null ? _null.hashCode() : 0);
			_result = 31 * _result + (var != null ? var.hashCode() : 0);
			_result = 31 * _result + (_abstract != null ? _abstract.hashCode() : 0);
			_result = 31 * _result + (_throws != null ? _throws.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C32Keyworded {" +
				"class=" + this._class + ", " +
				"long=" + this._long + ", " +
				"char=" + this._char + ", " +
				"this=" + this._this + ", " +
				"new=" + this._new + ", " +
				"private=" + this._private + ", " +
				"return=" + this._return + ", " +
				"package=" + this._package + ", " +
				"null=" + this._null + ", " +
				"var=" + this.var + ", " +
				"abstract=" + this._abstract + ", " +
				"throws=" + this._throws + ", " +
				"aux=" + this.aux +
			'}';
		}
	}

	/*********************** Builder Implementation of C32Keyworded  ***********************/
	class C32KeywordedBuilderImpl implements C32Keyworded.C32KeywordedBuilder {
	
		protected String _class;
		protected BigDecimal _long;
		protected String _char;
		protected String _this;
		protected List<String> _new = new ArrayList<>();
		protected String _private;
		protected BigDecimal _return;
		protected String _package;
		protected String _null;
		protected List<String> var = new ArrayList<>();
		protected Boolean _abstract;
		protected String _throws;
		protected C32Aux.C32AuxBuilder aux;
		
		@Override
		@RosettaAttribute("class")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("class")
		public String _getClass() {
			return _class;
		}
		
		@Override
		@RosettaAttribute("long")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("long")
		public BigDecimal getLong() {
			return _long;
		}
		
		@Override
		@RosettaAttribute("char")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("char")
		public String getChar() {
			return _char;
		}
		
		@Override
		@RosettaAttribute("this")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("this")
		public String getThis() {
			return _this;
		}
		
		@Override
		@RosettaAttribute("new")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("new")
		public List<String> getNew() {
			return _new;
		}
		
		@Override
		@RosettaAttribute("private")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("private")
		public String getPrivate() {
			return _private;
		}
		
		@Override
		@RosettaAttribute("return")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("return")
		public BigDecimal getReturn() {
			return _return;
		}
		
		@Override
		@RosettaAttribute("package")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("package")
		public String getPackage() {
			return _package;
		}
		
		@Override
		@RosettaAttribute("null")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("null")
		public String getNull() {
			return _null;
		}
		
		@Override
		@RosettaAttribute("var")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("var")
		public List<String> getVar() {
			return var;
		}
		
		@Override
		@RosettaAttribute("abstract")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("abstract")
		public Boolean getAbstract() {
			return _abstract;
		}
		
		@Override
		@RosettaAttribute("throws")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("throws")
		public String getThrows() {
			return _throws;
		}
		
		@Override
		@RosettaAttribute("aux")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("aux")
		public C32Aux.C32AuxBuilder getAux() {
			return aux;
		}
		
		@Override
		public C32Aux.C32AuxBuilder getOrCreateAux() {
			C32Aux.C32AuxBuilder result;
			if (aux!=null) {
				result = aux;
			}
			else {
				result = aux = C32Aux.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("class")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("class")
		@Override
		public C32Keyworded.C32KeywordedBuilder setClass(String __class) {
			this._class = __class == null ? null : __class;
			return this;
		}
		
		@RosettaAttribute("long")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("long")
		@Override
		public C32Keyworded.C32KeywordedBuilder setLong(BigDecimal __long) {
			this._long = __long == null ? null : __long;
			return this;
		}
		
		@RosettaAttribute("char")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("char")
		@Override
		public C32Keyworded.C32KeywordedBuilder setChar(String __char) {
			this._char = __char == null ? null : __char;
			return this;
		}
		
		@RosettaAttribute("this")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("this")
		@Override
		public C32Keyworded.C32KeywordedBuilder setThis(String __this) {
			this._this = __this == null ? null : __this;
			return this;
		}
		
		@RosettaAttribute("new")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("new")
		@Override
		public C32Keyworded.C32KeywordedBuilder addNew(String __new) {
			if (__new != null) {
				this._new.add(__new);
			}
			return this;
		}
		
		@Override
		public C32Keyworded.C32KeywordedBuilder addNew(String __new, int idx) {
			getIndex(this._new, idx, () -> __new);
			return this;
		}
		
		@Override
		public C32Keyworded.C32KeywordedBuilder addNew(List<String> news) {
			if (news != null) {
				for (final String toAdd : news) {
					this._new.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("new")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("new")
		@Override
		public C32Keyworded.C32KeywordedBuilder setNew(List<String> news) {
			if (news == null) {
				this._new = new ArrayList<>();
			} else {
				this._new = news.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("private")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("private")
		@Override
		public C32Keyworded.C32KeywordedBuilder setPrivate(String __private) {
			this._private = __private == null ? null : __private;
			return this;
		}
		
		@RosettaAttribute("return")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("return")
		@Override
		public C32Keyworded.C32KeywordedBuilder setReturn(BigDecimal __return) {
			this._return = __return == null ? null : __return;
			return this;
		}
		
		@RosettaAttribute("package")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("package")
		@Override
		public C32Keyworded.C32KeywordedBuilder setPackage(String __package) {
			this._package = __package == null ? null : __package;
			return this;
		}
		
		@RosettaAttribute("null")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("null")
		@Override
		public C32Keyworded.C32KeywordedBuilder setNull(String __null) {
			this._null = __null == null ? null : __null;
			return this;
		}
		
		@RosettaAttribute("var")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("var")
		@Override
		public C32Keyworded.C32KeywordedBuilder addVar(String _var) {
			if (_var != null) {
				this.var.add(_var);
			}
			return this;
		}
		
		@Override
		public C32Keyworded.C32KeywordedBuilder addVar(String _var, int idx) {
			getIndex(this.var, idx, () -> _var);
			return this;
		}
		
		@Override
		public C32Keyworded.C32KeywordedBuilder addVar(List<String> vars) {
			if (vars != null) {
				for (final String toAdd : vars) {
					this.var.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("var")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("var")
		@Override
		public C32Keyworded.C32KeywordedBuilder setVar(List<String> vars) {
			if (vars == null) {
				this.var = new ArrayList<>();
			} else {
				this.var = vars.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("abstract")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("abstract")
		@Override
		public C32Keyworded.C32KeywordedBuilder setAbstract(Boolean __abstract) {
			this._abstract = __abstract == null ? null : __abstract;
			return this;
		}
		
		@RosettaAttribute("throws")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("throws")
		@Override
		public C32Keyworded.C32KeywordedBuilder setThrows(String __throws) {
			this._throws = __throws == null ? null : __throws;
			return this;
		}
		
		@RosettaAttribute("aux")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("aux")
		@Override
		public C32Keyworded.C32KeywordedBuilder setAux(C32Aux _aux) {
			this.aux = _aux == null ? null : _aux.toBuilder();
			return this;
		}
		
		@Override
		public C32Keyworded build() {
			return new C32Keyworded.C32KeywordedImpl(this);
		}
		
		@Override
		public C32Keyworded.C32KeywordedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C32Keyworded.C32KeywordedBuilder prune() {
			if (aux!=null && !aux.prune().hasData()) aux = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (_getClass()!=null) return true;
			if (getLong()!=null) return true;
			if (getChar()!=null) return true;
			if (getThis()!=null) return true;
			if (getNew()!=null && !getNew().isEmpty()) return true;
			if (getPrivate()!=null) return true;
			if (getReturn()!=null) return true;
			if (getPackage()!=null) return true;
			if (getNull()!=null) return true;
			if (getVar()!=null && !getVar().isEmpty()) return true;
			if (getAbstract()!=null) return true;
			if (getThrows()!=null) return true;
			if (getAux()!=null && getAux().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C32Keyworded.C32KeywordedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C32Keyworded.C32KeywordedBuilder o = (C32Keyworded.C32KeywordedBuilder) other;
			
			merger.mergeRosetta(getAux(), o.getAux(), this::setAux);
			
			merger.mergeBasic(_getClass(), o._getClass(), this::setClass);
			merger.mergeBasic(getLong(), o.getLong(), this::setLong);
			merger.mergeBasic(getChar(), o.getChar(), this::setChar);
			merger.mergeBasic(getThis(), o.getThis(), this::setThis);
			merger.mergeBasic(getNew(), o.getNew(), (Consumer<String>) this::addNew);
			merger.mergeBasic(getPrivate(), o.getPrivate(), this::setPrivate);
			merger.mergeBasic(getReturn(), o.getReturn(), this::setReturn);
			merger.mergeBasic(getPackage(), o.getPackage(), this::setPackage);
			merger.mergeBasic(getNull(), o.getNull(), this::setNull);
			merger.mergeBasic(getVar(), o.getVar(), (Consumer<String>) this::addVar);
			merger.mergeBasic(getAbstract(), o.getAbstract(), this::setAbstract);
			merger.mergeBasic(getThrows(), o.getThrows(), this::setThrows);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C32Keyworded _that = getType().cast(o);
		
			if (!Objects.equals(_class, _that._getClass())) return false;
			if (!Objects.equals(_long, _that.getLong())) return false;
			if (!Objects.equals(_char, _that.getChar())) return false;
			if (!Objects.equals(_this, _that.getThis())) return false;
			if (!ListEquals.listEquals(_new, _that.getNew())) return false;
			if (!Objects.equals(_private, _that.getPrivate())) return false;
			if (!Objects.equals(_return, _that.getReturn())) return false;
			if (!Objects.equals(_package, _that.getPackage())) return false;
			if (!Objects.equals(_null, _that.getNull())) return false;
			if (!ListEquals.listEquals(var, _that.getVar())) return false;
			if (!Objects.equals(_abstract, _that.getAbstract())) return false;
			if (!Objects.equals(_throws, _that.getThrows())) return false;
			if (!Objects.equals(aux, _that.getAux())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (_class != null ? _class.hashCode() : 0);
			_result = 31 * _result + (_long != null ? _long.hashCode() : 0);
			_result = 31 * _result + (_char != null ? _char.hashCode() : 0);
			_result = 31 * _result + (_this != null ? _this.hashCode() : 0);
			_result = 31 * _result + (_new != null ? _new.hashCode() : 0);
			_result = 31 * _result + (_private != null ? _private.hashCode() : 0);
			_result = 31 * _result + (_return != null ? _return.hashCode() : 0);
			_result = 31 * _result + (_package != null ? _package.hashCode() : 0);
			_result = 31 * _result + (_null != null ? _null.hashCode() : 0);
			_result = 31 * _result + (var != null ? var.hashCode() : 0);
			_result = 31 * _result + (_abstract != null ? _abstract.hashCode() : 0);
			_result = 31 * _result + (_throws != null ? _throws.hashCode() : 0);
			_result = 31 * _result + (aux != null ? aux.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C32KeywordedBuilder {" +
				"class=" + this._class + ", " +
				"long=" + this._long + ", " +
				"char=" + this._char + ", " +
				"this=" + this._this + ", " +
				"new=" + this._new + ", " +
				"private=" + this._private + ", " +
				"return=" + this._return + ", " +
				"package=" + this._package + ", " +
				"null=" + this._null + ", " +
				"var=" + this.var + ", " +
				"abstract=" + this._abstract + ", " +
				"throws=" + this._throws + ", " +
				"aux=" + this.aux +
			'}';
		}
	}
}

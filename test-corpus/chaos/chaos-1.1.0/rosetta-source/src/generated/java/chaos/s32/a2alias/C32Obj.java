package chaos.s32.a2alias;

import chaos.s32.a2alias.meta.C32ObjMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * An attribute named o - upstream&#39;s equals(Object o) shadows the field it reads (the #624 Clash.java class, reproduced bug-compat) - and one named Class (getClass()).
 * @version 1.0.0
 */
@RosettaDataType(value="C32Obj", builder=C32Obj.C32ObjBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C32Obj", model="chaos", builder=C32Obj.C32ObjBuilderImpl.class, version="1.0.0")
public interface C32Obj extends RosettaModelObject {

	C32ObjMeta metaData = new C32ObjMeta();

	/*********************** Getter Methods  ***********************/
	String getO();
	String _getClass();

	/*********************** Build Methods  ***********************/
	C32Obj build();
	
	C32Obj.C32ObjBuilder toBuilder();
	
	static C32Obj.C32ObjBuilder builder() {
		return new C32Obj.C32ObjBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C32Obj> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C32Obj> getType() {
		return C32Obj.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("o"), String.class, getO(), this);
		processor.processBasic(path.newSubPath("Class"), String.class, _getClass(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C32ObjBuilder extends C32Obj, RosettaModelObjectBuilder {
		C32Obj.C32ObjBuilder setO(String o);
		C32Obj.C32ObjBuilder setClass(String Class);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("o"), String.class, getO(), this);
			processor.processBasic(path.newSubPath("Class"), String.class, _getClass(), this);
		}
		

		C32Obj.C32ObjBuilder prune();
	}

	/*********************** Immutable Implementation of C32Obj  ***********************/
	class C32ObjImpl implements C32Obj {
		private final String o;
		private final String _class;
		
		protected C32ObjImpl(C32Obj.C32ObjBuilder builder) {
			this.o = builder.getO();
			this._class = builder._getClass();
		}
		
		@Override
		@RosettaAttribute("o")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("o")
		public String getO() {
			return o;
		}
		
		@Override
		@RosettaAttribute("Class")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Class")
		public String _getClass() {
			return _class;
		}
		
		@Override
		public C32Obj build() {
			return this;
		}
		
		@Override
		public C32Obj.C32ObjBuilder toBuilder() {
			C32Obj.C32ObjBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C32Obj.C32ObjBuilder builder) {
			ofNullable(getO()).ifPresent(builder::setO);
			ofNullable(_getClass()).ifPresent(builder::setClass);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C32Obj _that = getType().cast(o);
		
			if (!Objects.equals(o, _that.getO())) return false;
			if (!Objects.equals(_class, _that._getClass())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (o != null ? o.hashCode() : 0);
			_result = 31 * _result + (_class != null ? _class.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C32Obj {" +
				"o=" + this.o + ", " +
				"Class=" + this._class +
			'}';
		}
	}

	/*********************** Builder Implementation of C32Obj  ***********************/
	class C32ObjBuilderImpl implements C32Obj.C32ObjBuilder {
	
		protected String o;
		protected String _class;
		
		@Override
		@RosettaAttribute("o")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("o")
		public String getO() {
			return o;
		}
		
		@Override
		@RosettaAttribute("Class")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("Class")
		public String _getClass() {
			return _class;
		}
		
		@RosettaAttribute("o")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("o")
		@Override
		public C32Obj.C32ObjBuilder setO(String _o) {
			this.o = _o == null ? null : _o;
			return this;
		}
		
		@RosettaAttribute("Class")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("Class")
		@Override
		public C32Obj.C32ObjBuilder setClass(String __class) {
			this._class = __class == null ? null : __class;
			return this;
		}
		
		@Override
		public C32Obj build() {
			return new C32Obj.C32ObjImpl(this);
		}
		
		@Override
		public C32Obj.C32ObjBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C32Obj.C32ObjBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getO()!=null) return true;
			if (_getClass()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C32Obj.C32ObjBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C32Obj.C32ObjBuilder o = (C32Obj.C32ObjBuilder) other;
			
			
			merger.mergeBasic(getO(), o.getO(), this::setO);
			merger.mergeBasic(_getClass(), o._getClass(), this::setClass);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C32Obj _that = getType().cast(o);
		
			if (!Objects.equals(o, _that.getO())) return false;
			if (!Objects.equals(_class, _that._getClass())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (o != null ? o.hashCode() : 0);
			_result = 31 * _result + (_class != null ? _class.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C32ObjBuilder {" +
				"o=" + this.o + ", " +
				"Class=" + this._class +
			'}';
		}
	}
}
